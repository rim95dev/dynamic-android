package io.github.rim95dev.island.overlay

import android.view.Surface

data class PxRect(val left: Int, val top: Int, val right: Int, val bottom: Int) {
    val width: Int get() = right - left
    val height: Int get() = bottom - top
    val centerX: Int get() = (left + right) / 2
    val centerY: Int get() = (top + bottom) / 2
}

/** The front camera hole in current screen coordinates. */
data class CameraHole(val centerX: Int, val centerY: Int, val diameter: Int)

/** A screen whose camera hole the system does not report, measured on the device in its natural orientation. */
private data class ScreenProfile(
    val modelPrefix: String,
    val naturalWidth: Int,
    val naturalHeight: Int,
    val hole: PxRect,
)

private val SCREEN_PROFILES = listOf(
    // Galaxy Z Fold8 inner screen. The public cutout API reports no cutout for it.
    ScreenProfile(modelPrefix = "SM-F971", naturalWidth = 2448, naturalHeight = 1848, hole = PxRect(1823, 18, 1901, 96)),
)

object CameraHoleResolver {

    private const val FALLBACK_CENTER_Y_DP = 20f
    private const val FALLBACK_DIAMETER_DP = 24f

    /**
     * Finds the camera hole of the current screen, on whichever edge the rotation put it.
     *
     * Uses a measured [ScreenProfile] first, then the [systemCutout] for the current rotation, and falls back
     * to the top center when neither knows the hole.
     */
    fun resolve(
        model: String,
        naturalWidth: Int,
        naturalHeight: Int,
        rotation: Int,
        screenWidth: Int,
        screenHeight: Int,
        systemCutout: PxRect?,
        density: Float,
    ): CameraHole {
        val profileHole = SCREEN_PROFILES
            .firstOrNull {
                model.startsWith(it.modelPrefix) &&
                    it.naturalWidth == naturalWidth &&
                    it.naturalHeight == naturalHeight
            }
            ?.let { rotate(it.hole, rotation, it.naturalWidth, it.naturalHeight) }

        val hole = (profileHole ?: systemCutout)?.takeIf { it.isOnScreen(screenWidth, screenHeight) }
        return if (hole != null) {
            CameraHole(centerX = hole.centerX, centerY = hole.centerY, diameter = minOf(hole.width, hole.height))
        } else {
            CameraHole(
                centerX = screenWidth / 2,
                centerY = (FALLBACK_CENTER_Y_DP * density).toInt(),
                diameter = (FALLBACK_DIAMETER_DP * density).toInt(),
            )
        }
    }

    /** Maps [rect] from the natural orientation to the given display rotation. */
    fun rotate(rect: PxRect, rotation: Int, naturalWidth: Int, naturalHeight: Int): PxRect = when (rotation) {
        Surface.ROTATION_90 -> PxRect(rect.top, naturalWidth - rect.right, rect.bottom, naturalWidth - rect.left)
        Surface.ROTATION_180 -> PxRect(
            naturalWidth - rect.right,
            naturalHeight - rect.bottom,
            naturalWidth - rect.left,
            naturalHeight - rect.top,
        )
        Surface.ROTATION_270 -> PxRect(naturalHeight - rect.bottom, rect.left, naturalHeight - rect.top, rect.right)
        else -> rect
    }

    /** Maps a point from the natural orientation to the given display rotation, like [rotate]. */
    fun rotatePoint(x: Int, y: Int, rotation: Int, naturalWidth: Int, naturalHeight: Int): Pair<Int, Int> =
        when (rotation) {
            Surface.ROTATION_90 -> y to naturalWidth - x
            Surface.ROTATION_180 -> naturalWidth - x to naturalHeight - y
            Surface.ROTATION_270 -> naturalHeight - y to x
            else -> x to y
        }

    /** Maps a point on the rotated screen back to the natural orientation. The inverse of [rotatePoint]. */
    fun unrotatePoint(x: Int, y: Int, rotation: Int, naturalWidth: Int, naturalHeight: Int): Pair<Int, Int> =
        when (rotation) {
            Surface.ROTATION_90 -> naturalWidth - y to x
            Surface.ROTATION_180 -> naturalWidth - x to naturalHeight - y
            Surface.ROTATION_270 -> y to naturalHeight - x
            else -> x to y
        }

    private fun PxRect.isOnScreen(screenWidth: Int, screenHeight: Int): Boolean =
        width > 0 && height > 0 && centerX in 0..screenWidth && centerY in 0..screenHeight
}
