package io.github.rim95dev.island.notification

import io.github.rim95dev.island.settings.AppFilter

/** What the island needs to know about a posted notification to decide whether to show it. */
data class NotificationFacts(
    val packageName: String,
    /** Ongoing or foreground-service notifications such as media playback, downloads, calls and alarms. */
    val isOngoing: Boolean,
    val isGroupSummary: Boolean,
    /** The channel importance is below default, so the system posts it without sound or pop-up. */
    val isSilent: Boolean,
    /** Do Not Disturb keeps this notification from alerting. */
    val isBlockedByDnd: Boolean,
    /** An update to a notification that is already posted and asked to alert only once. */
    val isQuietUpdate: Boolean,
    val isScreenOn: Boolean,
    /** The device is locked and the notification must not appear on the lock screen. */
    val isHiddenOnLockScreen: Boolean,
)

object NotificationGate {

    /** Shows only newly alerting notifications from apps the [appFilter] allows, while the screen is on. */
    fun shouldShow(facts: NotificationFacts, appFilter: AppFilter, ownPackageName: String): Boolean =
        facts.isScreenOn &&
            facts.packageName != ownPackageName &&
            appFilter.allows(facts.packageName) &&
            !facts.isOngoing &&
            !facts.isGroupSummary &&
            !facts.isSilent &&
            !facts.isBlockedByDnd &&
            !facts.isQuietUpdate &&
            !facts.isHiddenOnLockScreen
}
