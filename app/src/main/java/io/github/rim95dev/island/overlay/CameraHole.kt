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
     * Finds the camera hole on the top edge of the current screen.
     *
     * Uses a measured [ScreenProfile] first, then the system cutout, and falls back to the top center
     * when the screen has no hole on its top edge.
     */
    fun resolve(
        model: String,
        naturalWidth: Int,
        naturalHeight: Int,
        rotation: Int,
        screenWidth: Int,
        screenHeight: Int,
        systemCutoutTop: PxRect?,
        density: Float,
    ): CameraHole {
        val profileHole = SCREEN_PROFILES
            .firstOrNull {
                model.startsWith(it.modelPrefix) &&
                    it.naturalWidth == naturalWidth &&
                    it.naturalHeight == naturalHeight
            }
            ?.let { rotate(it.hole, rotation, it.naturalWidth, it.naturalHeight) }

        val hole = (profileHole ?: systemCutoutTop)?.takeIf { it.isOnTopEdge(screenHeight) }
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

    private fun PxRect.isOnTopEdge(screenHeight: Int): Boolean = height > 0 && width > 0 && top < screenHeight / 8
}
