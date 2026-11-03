package io.github.rim95dev.island.settings

import android.content.Context
import androidx.core.content.edit
import io.github.rim95dev.island.overlay.IslandAnimationStyle
import io.github.rim95dev.island.overlay.IslandMotion
import io.github.rim95dev.island.overlay.IslandPlacement
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet

/** Process-wide settings shared by the settings screen and the island services. */
class IslandSettings private constructor(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _appFilter = MutableStateFlow(readAppFilter())
    val appFilter: StateFlow<AppFilter> = _appFilter.asStateFlow()

    private val _cameraCalibrations = MutableStateFlow(readCameraCalibrations())

    /** User-adjusted camera holes keyed by screen. See [io.github.rim95dev.island.overlay.ScreenCamera.key]. */
    val cameraCalibrations: StateFlow<Map<String, CameraCalibration>> = _cameraCalibrations.asStateFlow()

    /** Saves the camera hole for [screenKey], or goes back to the detected one when [calibration] is null. */
    fun setCameraCalibration(screenKey: String, calibration: CameraCalibration?) {
        _cameraCalibrations.update { if (calibration == null) it - screenKey else it + (screenKey to calibration) }
        val legacyKeys = prefs.all.keys.filter { it.startsWith("$KEY_CAMERA_PREFIX$screenKey@") }
        prefs.edit {
            legacyKeys.forEach(::remove)
            if (calibration == null) remove(KEY_CAMERA_PREFIX + screenKey)
            else putString(KEY_CAMERA_PREFIX + screenKey, calibration.encode())
        }
    }

    private val _placement = MutableStateFlow(
        prefs.getString(KEY_PLACEMENT, null)
            ?.let { name -> IslandPlacement.entries.firstOrNull { it.name == name } }
            ?: IslandPlacement.FOLLOW_CAMERA,
    )
    val placement: StateFlow<IslandPlacement> = _placement.asStateFlow()

    fun setPlacement(placement: IslandPlacement) {
        _placement.value = placement
        prefs.edit { putString(KEY_PLACEMENT, placement.name) }
    }

    private val _animationStyle = MutableStateFlow(
        prefs.getString(KEY_ANIMATION_STYLE, null)
            ?.let { name -> IslandAnimationStyle.entries.firstOrNull { it.name == name } }
            ?: IslandAnimationStyle.STYLE_1,
    )
    val animationStyle: StateFlow<IslandAnimationStyle> = _animationStyle.asStateFlow()

    fun setAnimationStyle(style: IslandAnimationStyle) {
        _animationStyle.value = style
        prefs.edit { putString(KEY_ANIMATION_STYLE, style.name) }
    }

    private val _style2PauseMillis = MutableStateFlow(
        prefs.getInt(KEY_STYLE_2_PAUSE, IslandMotion.DEFAULT_PAUSE_MILLIS),
    )

    /** How long Style 2 waits as a pill before dropping into the card. */
    val style2PauseMillis: StateFlow<Int> = _style2PauseMillis.asStateFlow()

    fun setStyle2PauseMillis(millis: Int) {
        val clamped = millis.coerceIn(0, IslandMotion.MAX_PAUSE_MILLIS)
        _style2PauseMillis.value = clamped
        prefs.edit { putInt(KEY_STYLE_2_PAUSE, clamped) }
    }

    private val _keywordRules = MutableStateFlow(readKeywordRules())

    /** Keyword rules keyed by package name. Only rules with at least one keyword are kept. */
    val keywordRules: StateFlow<Map<String, KeywordRule>> = _keywordRules.asStateFlow()

    /** Saves the keyword rule for [packageName], or removes it when [rule] is null or has no keywords. */
    fun setKeywordRule(packageName: String, rule: KeywordRule?) {
        val kept = rule?.takeIf { it.keywords.isNotEmpty() }
        _keywordRules.update { if (kept == null) it - packageName else it + (packageName to kept) }
        prefs.edit {
            if (kept == null) remove(KEY_KEYWORD_RULE_PREFIX + packageName)
            else putString(KEY_KEYWORD_RULE_PREFIX + packageName, kept.encode())
        }
    }

    private val _oneTimeCode = MutableStateFlow(
        OneTimeCodeSettings(
            enabled = prefs.getBoolean(KEY_CODE_ENABLED, true),
            autoCopy = prefs.getBoolean(KEY_CODE_AUTO_COPY, true),
            keywords = prefs.getString(KEY_CODE_KEYWORDS, null)?.split("\n")?.filter { it.isNotEmpty() }.orEmpty(),
        ),
    )
    val oneTimeCode: StateFlow<OneTimeCodeSettings> = _oneTimeCode.asStateFlow()

    fun setOneTimeCodeEnabled(enabled: Boolean) = updateOneTimeCode { it.copy(enabled = enabled) }

    fun setOneTimeCodeAutoCopy(autoCopy: Boolean) = updateOneTimeCode { it.copy(autoCopy = autoCopy) }

    fun addOneTimeCodeKeyword(keyword: String) = updateOneTimeCode { it.copy(keywords = it.keywords.plusKeyword(keyword)) }

    fun removeOneTimeCodeKeyword(keyword: String) = updateOneTimeCode { it.copy(keywords = it.keywords - keyword) }

    private fun updateOneTimeCode(transform: (OneTimeCodeSettings) -> OneTimeCodeSettings) {
        val updated = _oneTimeCode.updateAndGet(transform)
        prefs.edit {
            putBoolean(KEY_CODE_ENABLED, updated.enabled)
            putBoolean(KEY_CODE_AUTO_COPY, updated.autoCopy)
            putString(KEY_CODE_KEYWORDS, updated.keywords.joinToString("\n"))
        }
    }

    fun setAppFilterMode(mode: AppFilterMode) = updateAppFilter { it.copy(mode = mode) }

    fun setAppException(packageName: String, isException: Boolean) =
        updateAppFilter { it.withException(packageName, isException) }

    private fun updateAppFilter(transform: (AppFilter) -> AppFilter) {
        val updated = _appFilter.updateAndGet(transform)
        prefs.edit {
            putString(KEY_MODE, updated.mode.name)
            putStringSet(KEY_EXCLUDED, updated.excludedPackages)
            putStringSet(KEY_INCLUDED, updated.includedPackages)
        }
    }

    private fun readAppFilter(): AppFilter = AppFilter(
        mode = prefs.getString(KEY_MODE, null)
            ?.let { name -> AppFilterMode.entries.firstOrNull { it.name == name } }
            ?: AppFilterMode.ALL_APPS,
        excludedPackages = prefs.getStringSet(KEY_EXCLUDED, null)?.toSet().orEmpty(),
        includedPackages = prefs.getStringSet(KEY_INCLUDED, null)?.toSet().orEmpty(),
    )

    private fun readKeywordRules(): Map<String, KeywordRule> =
        prefs.all
            .filterKeys { it.startsWith(KEY_KEYWORD_RULE_PREFIX) }
            .mapNotNull { (key, value) ->
                KeywordRule.decode(value as? String)?.let { key.removePrefix(KEY_KEYWORD_RULE_PREFIX) to it }
            }
            .toMap()

    private fun readCameraCalibrations(): Map<String, CameraCalibration> =
        CameraCalibration.fromSaved(
            prefs.all
                .filterKeys { it.startsWith(KEY_CAMERA_PREFIX) }
                .mapNotNull { (key, value) ->
                    CameraCalibration.decode(value as? String)?.let { key.removePrefix(KEY_CAMERA_PREFIX) to it }
                },
        )

    companion object {
        private const val PREFS_NAME = "island_settings"
        private const val KEY_CAMERA_PREFIX = "camera_center/"
        private const val KEY_PLACEMENT = "island_placement"
        private const val KEY_ANIMATION_STYLE = "animation_style"
        private const val KEY_STYLE_2_PAUSE = "style_2_pause_millis"
        private const val KEY_MODE = "app_filter_mode"
        private const val KEY_EXCLUDED = "excluded_packages"
        private const val KEY_INCLUDED = "included_packages"
        private const val KEY_KEYWORD_RULE_PREFIX = "keyword_rule/"
        private const val KEY_CODE_ENABLED = "one_time_code_enabled"
        private const val KEY_CODE_AUTO_COPY = "one_time_code_auto_copy"
        private const val KEY_CODE_KEYWORDS = "one_time_code_keywords"

        @Volatile
        private var instance: IslandSettings? = null

        fun get(context: Context): IslandSettings =
            instance ?: synchronized(this) {
                instance ?: IslandSettings(context.applicationContext).also { instance = it }
            }
    }
}
