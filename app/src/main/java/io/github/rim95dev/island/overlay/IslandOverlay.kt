package io.github.rim95dev.island.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.graphics.Region
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import io.github.rim95dev.island.notification.IslandNotification
import io.github.rim95dev.island.settings.IslandSettingsProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** State read by [IslandContent]. */
class IslandState {
    var geometry by mutableStateOf<IslandGeometry?>(null)
    var motion by mutableStateOf(IslandMotion(IslandAnimationStyle.STYLE_1))
    var notification by mutableStateOf<IslandNotification?>(null)
    var expanded by mutableStateOf(false)
}

/**
 * Shows the island in an accessibility overlay window, which is drawn above the status bar.
 *
 * The window stays attached for the whole service lifetime and shrinks to an untouchable 1×1 pixel while
 * idle. Samsung stops animation callbacks a few seconds after a process has no visible window, so removing
 * the window between notifications would freeze the next island at its first frame.
 *
 * [context] must be the accessibility service.
 */
class IslandOverlay(
    private val context: Context,
    private val scope: CoroutineScope,
    private val onOpen: (IslandNotification) -> Unit,
) {
    private val windowManager = context.getSystemService(WindowManager::class.java)
    private val state = IslandState()
    private val owner = OverlayLifecycleOwner()
    private val view = ComposeView(context).apply {
        setViewTreeLifecycleOwner(owner)
        setViewTreeSavedStateRegistryOwner(owner)
        setContent {
            IslandContent(state = state, onTap = { state.notification?.let(onOpen) })
        }
    }
    private val params = WindowManager.LayoutParams(
        1,
        1,
        WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
        IDLE_FLAGS,
        PixelFormat.TRANSLUCENT,
    ).apply {
        gravity = Gravity.TOP or Gravity.START
        layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
        fitInsetsTypes = 0
        title = WINDOW_TITLE
    }
    private var attached = false
    private var showing = false
    private var hideJob: Job? = null

    fun attach() {
        if (attached) return
        owner.start()
        windowManager.addView(view, params)
        attached = true
    }

    fun show(notification: IslandNotification) {
        attach()
        if (!showing) {
            val geometry = resolveGeometry()
            state.geometry = geometry
            state.motion = IslandMotion(
                style = IslandSettingsProvider.readAnimationStyle(context),
                pauseMillis = IslandSettingsProvider.readStyle2PauseMillis(context),
            )
            updateWindow(geometry.windowWidth, geometry.windowHeight, SHOWN_FLAGS)
            // The window spans the screen width; only the card takes touches so the rest of the status bar works.
            view.rootSurfaceControl?.setTouchableRegion(geometry.expanded.toRegion())
            showing = true
        }
        state.notification = notification
        state.expanded = true
        hideJob?.cancel()
        hideJob = scope.launch {
            delay(DISPLAY_MILLIS)
            hide()
        }
    }

    fun hideIfShowing(key: String) {
        if (showing && state.notification?.key == key) hide()
    }

    /** Shrinks the island back into the camera hole, then idles the window. */
    fun hide() {
        if (!showing) return
        hideJob?.cancel()
        state.expanded = false
        hideJob = scope.launch {
            delay(state.motion.collapseMillis + IDLE_DELAY_MILLIS)
            idle()
        }
    }

    /** Idles the window without animation, for example when the screen turns off or the phone folds. */
    fun hideNow() {
        hideJob?.cancel()
        state.expanded = false
        idle()
    }

    fun destroy() {
        hideJob?.cancel()
        if (attached) {
            windowManager.removeView(view)
            attached = false
        }
        owner.destroy()
    }

    private fun idle() {
        showing = false
        state.notification = null
        if (attached) updateWindow(1, 1, IDLE_FLAGS)
    }

    /** Resizes the window in place. It never moves, because the system would animate the move. */
    private fun updateWindow(width: Int, height: Int, flags: Int) {
        params.width = width
        params.height = height
        params.flags = flags
        if (attached) windowManager.updateViewLayout(view, params)
    }

    private fun resolveGeometry(): IslandGeometry {
        val screen = detectScreenCamera(context)
        val circle = IslandSettingsProvider.readPlacement(context).place(
            circle = screen.circleFor(IslandSettingsProvider.readCameraCalibration(context, screen.key)),
            width = screen.width,
            height = screen.height,
            density = screen.density,
        )
        return IslandGeometryCalculator.calculate(screen.width, screen.height, circle, screen.density)
    }

    private fun PxRect.toRegion() = Region(left, top, right, bottom)

    private companion object {
        const val DISPLAY_MILLIS = 5_000L

        /** Extra time after the collapse animation before the window shrinks, so its last frame is shown. */
        const val IDLE_DELAY_MILLIS = 70L
        const val WINDOW_TITLE = "DynamicIsland"

        const val SHOWN_FLAGS = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        const val IDLE_FLAGS = SHOWN_FLAGS or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
    }
}

/** Lifecycle for a Compose view that lives in a window without an activity. */
private class OverlayLifecycleOwner : SavedStateRegistryOwner {
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry get() = savedStateController.savedStateRegistry

    fun start() {
        savedStateController.performRestore(null)
        lifecycleRegistry.currentState = Lifecycle.State.RESUMED
    }

    fun destroy() {
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
    }
}
