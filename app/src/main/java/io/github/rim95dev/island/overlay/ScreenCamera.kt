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
 * Foldables swap physical screens on the same display, so [key] names the physical screen by its natural
 * size. Camera positions adjusted by the user are saved once per [key] in natural-orientation coordinates
 * and follow the screen when it rotates.
 */
data class ScreenCamera(
    val naturalWidth: Int,
    val naturalHeight: Int,
    val rotation: Int,
    val width: Int,
    val height: Int,
    val density: Float,
    /** The detected hole in current screen coordinates. */
    val detectedHole: CameraHole,
) {
    val key: String get() = "${naturalWidth}x$naturalHeight"

    /** The island's circle size before the user adjusts it: the detected hole with a thin ring around it. */
    val defaultCircleDiameter: Int
        get() = detectedHole.diameter + 2 * (DEFAULT_RING_DP * density).toInt()

    /** The island's circle in current screen coordinates: the user's [calibration] where set, the detected hole otherwise. */
    fun circleFor(calibration: CameraCalibration?): CameraHole {
        if (calibration == null) return detectedHole.copy(diameter = defaultCircleDiameter)
        val (x, y) = CameraHoleResolver.rotatePoint(calibration.x, calibration.y, rotation, naturalWidth, naturalHeight)
        return CameraHole(centerX = x, centerY = y, diameter = calibration.diameter ?: defaultCircleDiameter)
    }

    /** Converts a point on the current screen to the natural-orientation coordinates a calibration is saved in. */
    fun toCalibration(x: Int, y: Int, diameter: Int): CameraCalibration {
        val (naturalX, naturalY) = CameraHoleResolver.unrotatePoint(x, y, rotation, naturalWidth, naturalHeight)
        return CameraCalibration(naturalX, naturalY, diameter)
    }

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
    val systemCutout = display.cutout?.let { cutout ->
        listOf(cutout.boundingRectTop, cutout.boundingRectLeft, cutout.boundingRectRight, cutout.boundingRectBottom)
            .firstOrNull { !it.isEmpty }
            ?.let { PxRect(it.left, it.top, it.right, it.bottom) }
    }
    val hole = CameraHoleResolver.resolve(
        model = Build.MODEL,
        naturalWidth = naturalWidth,
        naturalHeight = naturalHeight,
        rotation = display.rotation,
        screenWidth = bounds.width(),
        screenHeight = bounds.height(),
        systemCutout = systemCutout,
        density = density,
    )
    return ScreenCamera(
        naturalWidth = naturalWidth,
        naturalHeight = naturalHeight,
        rotation = display.rotation,
        width = bounds.width(),
        height = bounds.height(),
        density = density,
        detectedHole = hole,
    )
}
