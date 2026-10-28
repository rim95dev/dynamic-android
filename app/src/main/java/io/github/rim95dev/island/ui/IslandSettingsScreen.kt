package io.github.rim95dev.island.ui

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.rim95dev.island.R
import io.github.rim95dev.island.apps.LauncherApp
import io.github.rim95dev.island.apps.loadLauncherApps
import io.github.rim95dev.island.overlay.IslandAnimationStyle
import io.github.rim95dev.island.overlay.IslandMotion
import io.github.rim95dev.island.overlay.IslandPlacement
import io.github.rim95dev.island.service.IslandAccessibilityService
import io.github.rim95dev.island.settings.AppFilterMode
import io.github.rim95dev.island.settings.IslandSettings
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun IslandSettingsScreen(onAdjustCamera: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val settings = remember { IslandSettings.get(context) }
    val appFilter by settings.appFilter.collectAsStateWithLifecycle()
    val placement by settings.placement.collectAsStateWithLifecycle()
    val animationStyle by settings.animationStyle.collectAsStateWithLifecycle()
    val style2PauseMillis by settings.style2PauseMillis.collectAsStateWithLifecycle()
    val apps by produceState<List<LauncherApp>?>(initialValue = null) {
        value = withContext(Dispatchers.IO) { loadLauncherApps(context) }
    }
    var query by rememberSaveable { mutableStateOf("") }

    var appLanguage by remember { mutableStateOf(AppLanguage.current(context)) }
    var accessibilityEnabled by remember { mutableStateOf(false) }
    var notificationAccessEnabled by remember { mutableStateOf(false) }
    LifecycleResumeEffect(Unit) {
        accessibilityEnabled = context.isIslandAccessibilityServiceEnabled()
        notificationAccessEnabled = context.isNotificationListenerEnabled()
        onPauseOrDispose { }
    }

    val visibleApps = apps?.filter {
        query.isBlank() ||
            it.label.contains(query, ignoreCase = true) ||
            it.packageName.contains(query, ignoreCase = true)
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { SectionTitle(R.string.section_language) }
        items(AppLanguage.entries) { language ->
            LanguageOption(
                title = stringResource(language.labelRes),
                selected = language == appLanguage,
                onSelect = {
                    appLanguage = language
                    language.apply(context)
                },
            )
        }

        item { SectionTitle(R.string.section_permissions) }
        item {
            PermissionRow(
                title = stringResource(R.string.permission_accessibility),
                granted = accessibilityEnabled,
                onOpenSettings = { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) },
            )
        }
        item {
            PermissionRow(
                title = stringResource(R.string.permission_notification_access),
                granted = notificationAccessEnabled,
                onOpenSettings = { context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) },
            )
        }

        item { SectionTitle(R.string.section_camera_position) }
        item {
            ModeOption(
                title = stringResource(R.string.placement_follow_camera),
                summary = stringResource(R.string.placement_follow_camera_summary),
                selected = placement == IslandPlacement.FOLLOW_CAMERA,
                onSelect = { settings.setPlacement(IslandPlacement.FOLLOW_CAMERA) },
            )
        }
        item {
            ModeOption(
                title = stringResource(R.string.placement_top),
                summary = stringResource(R.string.placement_top_summary),
                selected = placement == IslandPlacement.TOP_ONLY,
                onSelect = { settings.setPlacement(IslandPlacement.TOP_ONLY) },
            )
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.camera_position_summary),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 12.dp),
                )
                Button(onClick = onAdjustCamera) {
                    Text(text = stringResource(R.string.camera_position_adjust))
                }
            }
        }

        item { SectionTitle(R.string.section_animation) }
        item {
            ModeOption(
                title = stringResource(R.string.animation_style_1),
                summary = stringResource(R.string.animation_style_1_summary),
                selected = animationStyle == IslandAnimationStyle.STYLE_1,
                onSelect = { settings.setAnimationStyle(IslandAnimationStyle.STYLE_1) },
            )
        }
        item {
            ModeOption(
                title = stringResource(R.string.animation_style_2),
                summary = stringResource(R.string.animation_style_2_summary),
                selected = animationStyle == IslandAnimationStyle.STYLE_2,
                onSelect = { settings.setAnimationStyle(IslandAnimationStyle.STYLE_2) },
            )
        }
        if (animationStyle == IslandAnimationStyle.STYLE_2) {
            item {
                Style2PauseControl(
                    savedMillis = style2PauseMillis,
                    onSave = settings::setStyle2PauseMillis,
                )
            }
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(
                        if (accessibilityEnabled) R.string.test_island_summary else R.string.test_island_needs_accessibility,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 12.dp),
                )
                Button(
                    onClick = { IslandAccessibilityService.showTestIsland(context) },
                    enabled = accessibilityEnabled,
                ) {
                    Text(text = stringResource(R.string.test_island_button))
                }
            }
        }

        item { SectionTitle(R.string.section_notification_apps) }
        item {
            ModeOption(
                title = stringResource(R.string.mode_all_apps),
                summary = stringResource(R.string.mode_all_apps_summary),
                selected = appFilter.mode == AppFilterMode.ALL_APPS,
                onSelect = { settings.setAppFilterMode(AppFilterMode.ALL_APPS) },
            )
        }
        item {
            ModeOption(
                title = stringResource(R.string.mode_no_apps),
                summary = stringResource(R.string.mode_no_apps_summary),
                selected = appFilter.mode == AppFilterMode.NO_APPS,
                onSelect = { settings.setAppFilterMode(AppFilterMode.NO_APPS) },
            )
        }

        item {
            val titleRes = when (appFilter.mode) {
                AppFilterMode.ALL_APPS -> R.string.exceptions_hidden_apps
                AppFilterMode.NO_APPS -> R.string.exceptions_shown_apps
            }
            Text(
                text = stringResource(titleRes, appFilter.exceptions.size),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text(stringResource(R.string.search_apps)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        if (visibleApps == null) {
            item {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        } else {
            items(visibleApps, key = { it.packageName }) { app ->
                AppExceptionRow(
                    app = app,
                    checked = app.packageName in appFilter.exceptions,
                    onCheckedChange = { settings.setAppException(app.packageName, it) },
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(@StringRes textRes: Int) {
    Text(
        text = stringResource(textRes),
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier.padding(top = 8.dp),
    )
}

@Composable
private fun PermissionRow(title: String, granted: Boolean, onOpenSettings: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = stringResource(if (granted) R.string.permission_granted else R.string.permission_denied),
                style = MaterialTheme.typography.bodyMedium,
                color = if (granted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
            )
        }
        Button(onClick = onOpenSettings) {
            Text(text = stringResource(R.string.open_settings))
        }
    }
}

@Composable
private fun LanguageOption(title: String, selected: Boolean, onSelect: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onSelect, role = Role.RadioButton),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = 12.dp),
        )
    }
}

private val AppLanguage.labelRes: Int
    get() = when (this) {
        AppLanguage.SYSTEM -> R.string.language_system
        AppLanguage.ENGLISH -> R.string.language_english
        AppLanguage.KOREAN -> R.string.language_korean
    }

@Composable
private fun ModeOption(title: String, summary: String, selected: Boolean, onSelect: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onSelect, role = Role.RadioButton),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Column(modifier = Modifier.padding(start = 12.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Text(text = summary, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun Style2PauseControl(savedMillis: Int, onSave: (Int) -> Unit) {
    var millis by remember(savedMillis) { mutableFloatStateOf(savedMillis.toFloat()) }
    Column(modifier = Modifier.padding(start = 52.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.style_2_pause),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = stringResource(R.string.style_2_pause_value, millis.roundToInt()),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        Slider(
            value = millis,
            onValueChange = { millis = it },
            onValueChangeFinished = { onSave(millis.roundToInt()) },
            valueRange = 0f..IslandMotion.MAX_PAUSE_MILLIS.toFloat(),
            steps = IslandMotion.MAX_PAUSE_MILLIS / PAUSE_STEP_MILLIS - 1,
        )
        Text(text = stringResource(R.string.style_2_pause_summary), style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun AppExceptionRow(app: LauncherApp, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val icon = remember(app.packageName) { app.icon.toBitmap(ICON_SIZE_PX, ICON_SIZE_PX).asImageBitmap() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(value = checked, onValueChange = onCheckedChange, role = Role.Checkbox),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(bitmap = icon, contentDescription = null, modifier = Modifier.size(40.dp))
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp),
        ) {
            Text(text = app.label, style = MaterialTheme.typography.bodyLarge)
            Text(text = app.packageName, style = MaterialTheme.typography.bodySmall)
        }
        Checkbox(checked = checked, onCheckedChange = null)
    }
}

private const val ICON_SIZE_PX = 120
private const val PAUSE_STEP_MILLIS = 10

private fun Context.isIslandAccessibilityServiceEnabled(): Boolean =
    getSystemService(AccessibilityManager::class.java)
        .getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        .any {
            val service = it.resolveInfo.serviceInfo
            service.packageName == packageName && service.name == IslandAccessibilityService::class.java.name
        }

private fun Context.isNotificationListenerEnabled(): Boolean =
    NotificationManagerCompat.getEnabledListenerPackages(this).contains(packageName)
