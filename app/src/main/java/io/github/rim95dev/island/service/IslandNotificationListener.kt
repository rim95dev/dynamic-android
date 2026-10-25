package io.github.rim95dev.island.service

import android.app.KeyguardManager
import android.app.Notification
import android.app.NotificationManager
import android.os.Bundle
import android.os.PowerManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import io.github.rim95dev.island.R
import io.github.rim95dev.island.notification.IslandEvent
import io.github.rim95dev.island.notification.IslandEvents
import io.github.rim95dev.island.notification.IslandNotification
import io.github.rim95dev.island.notification.NotificationFacts
import io.github.rim95dev.island.notification.NotificationGate
import io.github.rim95dev.island.settings.IslandSettingsProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class IslandNotificationListener : NotificationListenerService() {

    private val activeKeys = mutableSetOf<String>()
    private var scope: CoroutineScope? = null

    override fun onListenerConnected() {
        activeKeys.clear()
        activeNotifications?.mapTo(activeKeys) { it.key }
        scope = MainScope().also {
            it.launch {
                IslandEvents.cancelRequests.collect { key -> cancelNotification(key) }
            }
        }
    }

    override fun onListenerDisconnected() {
        scope?.cancel()
        scope = null
    }

    override fun onNotificationPosted(sbn: StatusBarNotification, rankingMap: RankingMap) {
        val wasActive = !activeKeys.add(sbn.key)
        val ranking = Ranking().also { rankingMap.getRanking(sbn.key, it) }
        val notification = sbn.notification
        val isLocked = getSystemService(KeyguardManager::class.java).isKeyguardLocked
        val lockScreenVisibility = ranking.lockscreenVisibilityOverride
            .takeIf { it != Ranking.VISIBILITY_NO_OVERRIDE }
            ?: notification.visibility

        val facts = NotificationFacts(
            packageName = sbn.packageName,
            isOngoing = notification.hasFlag(Notification.FLAG_ONGOING_EVENT) ||
                notification.hasFlag(Notification.FLAG_FOREGROUND_SERVICE),
            isGroupSummary = notification.hasFlag(Notification.FLAG_GROUP_SUMMARY),
            isSilent = ranking.importance < NotificationManager.IMPORTANCE_DEFAULT,
            isBlockedByDnd = !ranking.matchesInterruptionFilter(),
            isQuietUpdate = wasActive && notification.hasFlag(Notification.FLAG_ONLY_ALERT_ONCE),
            isScreenOn = getSystemService(PowerManager::class.java).isInteractive,
            isHiddenOnLockScreen = isLocked && lockScreenVisibility == Notification.VISIBILITY_SECRET,
        )
        val appFilter = IslandSettingsProvider.readAppFilter(this)
        if (!NotificationGate.shouldShow(facts, appFilter, packageName)) return

        val hideContent = isLocked && lockScreenVisibility != Notification.VISIBILITY_PUBLIC
        IslandEvents.post(IslandEvent.Show(sbn.toIslandNotification(hideContent)))
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        activeKeys.remove(sbn.key)
        IslandEvents.post(IslandEvent.Remove(sbn.key))
    }

    private fun StatusBarNotification.toIslandNotification(hideContent: Boolean): IslandNotification {
        val appLabel = runCatching {
            packageManager.getApplicationLabel(packageManager.getApplicationInfo(packageName, 0)).toString()
        }.getOrDefault(packageName)
        val icon = runCatching { packageManager.getApplicationIcon(packageName) }.getOrNull()
            ?: notification.smallIcon?.loadDrawable(this@IslandNotificationListener)

        val (title, text) = if (hideContent) {
            // On the lock screen only the redacted public version may be shown.
            val publicText = notification.publicVersion?.extras?.text()
            appLabel to (publicText ?: getString(R.string.island_new_notification))
        } else {
            val extras = notification.extras
            (extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: appLabel) to extras.text()
        }

        return IslandNotification(
            key = key,
            packageName = packageName,
            title = title,
            text = text,
            icon = icon,
            contentIntent = notification.contentIntent,
            autoCancel = notification.hasFlag(Notification.FLAG_AUTO_CANCEL),
        )
    }
}

private fun Notification.hasFlag(flag: Int): Boolean = flags and flag != 0

private fun Bundle.text(): String? =
    (getCharSequence(Notification.EXTRA_TEXT) ?: getCharSequence(Notification.EXTRA_BIG_TEXT))?.toString()
