package io.github.rim95dev.island.overlay

import android.content.Context
import android.hardware.display.DisplayManager
import android.os.Build
import android.view.Display
import android.view.WindowManager
import io.github.rim95dev.island.settings.CameraCalibration

/**
 * The screen currently showing on the default display and its detected camera hole.
 *
 * Foldables swap physical screens on the same display, so [key] names the physical screen and its
 * rotation. Camera positions adjusted by the user are saved per [key].
 */
data class ScreenCamera(
    val key: String,
    val width: Int,
    val height: Int,
    val density: Float,
    val detectedHole: CameraHole,
) {
    /** The island's circle size before the user adjusts it: the detected hole with a thin ring around it. */
    val defaultCircleDiameter: Int
        get() = detectedHole.diameter + 2 * (DEFAULT_RING_DP * density).toInt()

    /** The island's circle on this screen: the user's [calibration] where set, the detected hole otherwise. */
    fun circleFor(calibration: CameraCalibration?) = CameraHole(
        centerX = calibration?.x ?: detectedHole.centerX,
        centerY = calibration?.y ?: detectedHole.centerY,
        diameter = calibration?.diameter ?: defaultCircleDiameter,
    )

    private companion object {
        const val DEFAULT_RING_DP = 5f
    }
}

fun detectScreenCamera(context: Context): ScreenCamera {
    val bounds = context.getSystemService(WindowManager::class.java).maximumWindowMetrics.bounds
    val display = context.getSystemService(DisplayManager::class.java).getDisplay(Display.DEFAULT_DISPLAY)
    val density = context.resources.displayMetrics.density
    val naturalWidth = display.mode.physicalWidth
    val naturalHeight = display.mode.physicalHeight
    val systemCutoutTop = display.cutout?.boundingRectTop
        ?.takeUnless { it.isEmpty }
        ?.let { PxRect(it.left, it.top, it.right, it.bottom) }
    val hole = CameraHoleResolver.resolve(
        model = Build.MODEL,
        naturalWidth = naturalWidth,
        naturalHeight = naturalHeight,
        rotation = display.rotation,
        screenWidth = bounds.width(),
        screenHeight = bounds.height(),
        systemCutoutTop = systemCutoutTop,
        density = density,
    )
    return ScreenCamera(
        key = "${naturalWidth}x$naturalHeight@${display.rotation}",
        width = bounds.width(),
        height = bounds.height(),
        density = density,
        detectedHole = hole,
    )
}
