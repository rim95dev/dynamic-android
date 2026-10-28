package io.github.rim95dev.island.settings

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import io.github.rim95dev.island.overlay.IslandAnimationStyle
import io.github.rim95dev.island.overlay.IslandMotion
import io.github.rim95dev.island.overlay.IslandPlacement

/**
 * Serves [IslandSettings] from the main process to the island services, which run in the `:island` process.
 *
 * Settings are written by the settings screen in the main process, so reading them through this provider
 * always returns the latest values instead of a stale per-process copy.
 */
class IslandSettingsProvider : ContentProvider() {

    override fun onCreate(): Boolean = true

    override fun call(method: String, arg: String?, extras: Bundle?): Bundle? {
        val context = context ?: return null
        val settings = IslandSettings.get(context)
        return when (method) {
            METHOD_GET_APP_FILTER -> settings.appFilter.value.toBundle()
            METHOD_GET_CAMERA_CALIBRATION -> arg?.let { settings.cameraCalibrations.value[it] }?.toBundle()
            METHOD_GET_PLACEMENT -> Bundle().apply { putString(KEY_PLACEMENT, settings.placement.value.name) }
            METHOD_GET_ANIMATION_STYLE -> Bundle().apply { putString(KEY_STYLE, settings.animationStyle.value.name) }
            METHOD_GET_STYLE_2_PAUSE -> Bundle().apply { putInt(KEY_PAUSE, settings.style2PauseMillis.value) }
            else -> null
        }
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
    ): Cursor? = null

    override fun getType(uri: Uri): String? = null

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0

    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = 0

    companion object {
        private const val METHOD_GET_APP_FILTER = "getAppFilter"
        private const val METHOD_GET_CAMERA_CALIBRATION = "getCameraCalibration"
        private const val METHOD_GET_PLACEMENT = "getPlacement"
        private const val KEY_PLACEMENT = "placement"
        private const val METHOD_GET_ANIMATION_STYLE = "getAnimationStyle"
        private const val METHOD_GET_STYLE_2_PAUSE = "getStyle2Pause"
        private const val KEY_PAUSE = "pause"
        private const val KEY_STYLE = "style"
        private const val KEY_MODE = "mode"
        private const val KEY_EXCLUDED = "excluded"
        private const val KEY_INCLUDED = "included"
        private const val KEY_X = "x"
        private const val KEY_Y = "y"
        private const val KEY_DIAMETER = "diameter"

        /** Reads the current app filter from the main process. Falls back to the default filter on failure. */
        fun readAppFilter(context: Context): AppFilter =
            call(context, METHOD_GET_APP_FILTER)?.toAppFilter() ?: AppFilter()

        /** Reads the user-adjusted camera hole for [screenKey], or null when the detected one is used. */
        fun readCameraCalibration(context: Context, screenKey: String): CameraCalibration? =
            call(context, METHOD_GET_CAMERA_CALIBRATION, screenKey)?.let {
                CameraCalibration(
                    x = it.getInt(KEY_X),
                    y = it.getInt(KEY_Y),
                    diameter = if (it.containsKey(KEY_DIAMETER)) it.getInt(KEY_DIAMETER) else null,
                )
            }

        /** Reads where the island opens. Falls back to following the camera on failure. */
        fun readPlacement(context: Context): IslandPlacement {
            val name = call(context, METHOD_GET_PLACEMENT)?.getString(KEY_PLACEMENT)
            return IslandPlacement.entries.firstOrNull { it.name == name } ?: IslandPlacement.FOLLOW_CAMERA
        }

        /** Reads the selected animation style. Falls back to the default style on failure. */
        fun readAnimationStyle(context: Context): IslandAnimationStyle {
            val name = call(context, METHOD_GET_ANIMATION_STYLE)?.getString(KEY_STYLE)
            return IslandAnimationStyle.entries.firstOrNull { it.name == name } ?: IslandAnimationStyle.STYLE_1
        }

        /** Reads how long Style 2 pauses. Falls back to the default pause on failure. */
        fun readStyle2PauseMillis(context: Context): Int =
            call(context, METHOD_GET_STYLE_2_PAUSE)?.getInt(KEY_PAUSE, IslandMotion.DEFAULT_PAUSE_MILLIS)
                ?: IslandMotion.DEFAULT_PAUSE_MILLIS

        private fun call(context: Context, method: String, arg: String? = null): Bundle? = runCatching {
            context.contentResolver.call("${context.packageName}.settings", method, arg, null)
        }.getOrNull()

        private fun CameraCalibration.toBundle() = Bundle().apply {
            putInt(KEY_X, x)
            putInt(KEY_Y, y)
            diameter?.let { putInt(KEY_DIAMETER, it) }
        }

        private fun AppFilter.toBundle() = Bundle().apply {
            putString(KEY_MODE, mode.name)
            putStringArrayList(KEY_EXCLUDED, ArrayList(excludedPackages))
            putStringArrayList(KEY_INCLUDED, ArrayList(includedPackages))
        }

        private fun Bundle.toAppFilter() = AppFilter(
            mode = AppFilterMode.entries.firstOrNull { it.name == getString(KEY_MODE) } ?: AppFilterMode.ALL_APPS,
            excludedPackages = getStringArrayList(KEY_EXCLUDED)?.toSet().orEmpty(),
            includedPackages = getStringArrayList(KEY_INCLUDED)?.toSet().orEmpty(),
        )
    }
}
