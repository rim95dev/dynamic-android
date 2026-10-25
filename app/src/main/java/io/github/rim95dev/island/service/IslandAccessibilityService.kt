package io.github.rim95dev.island.service

import android.accessibilityservice.AccessibilityService
import android.app.ActivityOptions
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.view.accessibility.AccessibilityEvent
import androidx.core.content.ContextCompat
import io.github.rim95dev.island.R
import io.github.rim95dev.island.notification.IslandEvent
import io.github.rim95dev.island.notification.IslandEvents
import io.github.rim95dev.island.notification.IslandNotification
import io.github.rim95dev.island.overlay.IslandOverlay
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class IslandAccessibilityService : AccessibilityService() {

    private val scope = MainScope()
    private var overlay: IslandOverlay? = null

    private val screenOffReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            overlay?.hideNow()
        }
    }

    private val testReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            overlay?.show(testNotification())
        }
    }

    override fun onServiceConnected() {
        val overlay = IslandOverlay(this, scope, ::open).also { overlay = it }
        overlay.attach()
        ContextCompat.registerReceiver(
            this,
            screenOffReceiver,
            IntentFilter(Intent.ACTION_SCREEN_OFF),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        ContextCompat.registerReceiver(
            this,
            testReceiver,
            IntentFilter(ACTION_SHOW_TEST),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        scope.launch {
            IslandEvents.events.collect { event ->
                when (event) {
                    is IslandEvent.Show -> overlay.show(event.notification)
                    is IslandEvent.Remove -> overlay.hideIfShowing(event.key)
                }
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // Folding, unfolding and rotating move the camera hole, so the current window is no longer aligned.
        overlay?.hideNow()
    }

    override fun onDestroy() {
        overlay?.let {
            it.destroy()
            unregisterReceiver(screenOffReceiver)
            unregisterReceiver(testReceiver)
        }
        overlay = null
        scope.cancel()
        super.onDestroy()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) = Unit

    override fun onInterrupt() = Unit

    private fun open(notification: IslandNotification) {
        overlay?.hide()
        notification.contentIntent?.let { intent ->
            val options = ActivityOptions.makeBasic()
                .setPendingIntentBackgroundActivityStartMode(
                    ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOW_ALWAYS,
                )
                .toBundle()
            try {
                intent.send(this, 0, null, null, null, null, options)
            } catch (_: PendingIntent.CanceledException) {
                return
            }
        }
        if (notification.autoCancel) IslandEvents.requestCancel(notification.key)
    }

    private fun testNotification() = IslandNotification(
        key = TEST_KEY,
        packageName = packageName,
        title = getString(R.string.test_island_title),
        text = getString(R.string.test_island_text),
        icon = packageManager.getApplicationIcon(packageName),
        contentIntent = null,
        autoCancel = false,
    )

    companion object {
        private const val ACTION_SHOW_TEST = "io.github.rim95dev.island.action.SHOW_TEST"
        private const val TEST_KEY = "island-test"

        /** Asks the running service to show a sample island. Does nothing while the service is off. */
        fun showTestIsland(context: Context) {
            context.sendBroadcast(Intent(ACTION_SHOW_TEST).setPackage(context.packageName))
        }
    }
}
