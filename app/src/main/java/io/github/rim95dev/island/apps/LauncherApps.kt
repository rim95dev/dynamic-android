package io.github.rim95dev.island.apps

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import java.text.Collator

data class LauncherApp(
    val packageName: String,
    val label: String,
    val icon: Drawable,
)

/** Apps that appear in the launcher, sorted by label. Requires the launcher `<queries>` entry in the manifest. */
fun loadLauncherApps(context: Context): List<LauncherApp> {
    val packageManager = context.packageManager
    val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    val collator = Collator.getInstance()
    return packageManager
        .queryIntentActivities(launcherIntent, PackageManager.ResolveInfoFlags.of(0))
        .asSequence()
        .map { it.activityInfo.applicationInfo }
        .filter { it.packageName != context.packageName }
        .distinctBy { it.packageName }
        .map {
            LauncherApp(
                packageName = it.packageName,
                label = packageManager.getApplicationLabel(it).toString(),
                icon = packageManager.getApplicationIcon(it),
            )
        }
        .sortedWith(compareBy(collator) { it.label })
        .toList()
}
