package io.github.rim95dev.island.overlay

import kotlin.math.roundToInt

/**
 * Where the island window sits on screen and how the island grows inside it.
 *
 * The island grows from a dot at the center of the camera hole ([collapsed]) into a circle around the hole
 * ([circle]), stretches sideways into a pill, and then drops down into a card ([expanded]) that keeps the
 * hole in its top area. [IslandMotion] decides when each step happens. Collapsing runs the same path
 * backwards, so the island is pulled back into the camera instead of flattening into a line. The content
 * starts below the hole at [contentTop].
 *
 * The window always starts at the top-left corner of the screen and spans its width, so all rects are in
 * screen coordinates. Keeping the window still matters: the system animates window moves, which would make
 * the island slide in from the side instead of growing out of the camera.
 */
data class IslandGeometry(
    val windowWidth: Int,
    val windowHeight: Int,
    val collapsed: PxRect,
    val circle: PxRect,
    val expanded: PxRect,
    /** Distance from the top of [expanded] to the top of the content. */
    val contentTop: Int,
    val contentHeight: Int,
) {
    /** The island bounds at [fraction] of the way from [collapsed] (0) to [expanded] (1) along the [motion] path. */
    fun boundsAt(fraction: Float, motion: IslandMotion = IslandMotion(IslandAnimationStyle.STYLE_1)): PxRect {
        val f = fraction.coerceIn(0f, 1f)
        val circleSize = circle.width.toFloat()
        val holeX = collapsed.left.toFloat()
        val holeY = collapsed.top.toFloat()

        if (f < motion.circleEnd) {
            val size = circleSize * (f / motion.circleEnd)
            return rectAround(holeX, holeY, size, size)
        }

        val stretch = smoothPhase(f, motion.circleEnd, motion.stretchEnd)
        val drop = smoothPhase(f, motion.dropStart, 1f)
        val width = lerp(circleSize, expanded.width.toFloat(), stretch)
        val height = lerp(circleSize, expanded.height.toFloat(), drop)
        val centerX = lerp(holeX, expanded.left + expanded.width / 2f, stretch)
        val top = lerp(circle.top.toFloat(), expanded.top.toFloat(), drop)
        return PxRect(
            left = (centerX - width / 2f).roundToInt(),
            top = top.roundToInt(),
            right = (centerX + width / 2f).roundToInt(),
            bottom = (top + height).roundToInt(),
        )
    }

    private fun rectAround(centerX: Float, centerY: Float, width: Float, height: Float) = PxRect(
        left = (centerX - width / 2f).roundToInt(),
        top = (centerY - height / 2f).roundToInt(),
        right = (centerX + width / 2f).roundToInt(),
        bottom = (centerY + height / 2f).roundToInt(),
    )

    private fun smoothPhase(fraction: Float, start: Float, end: Float): Float {
        val t = ((fraction - start) / (end - start)).coerceIn(0f, 1f)
        return t * t * (3f - 2f * t)
    }

    private fun lerp(start: Float, stop: Float, fraction: Float): Float = start + (stop - start) * fraction
}

object IslandGeometryCalculator {

    private const val SIDE_MARGIN_DP = 12f
    private const val MAX_WIDTH_DP = 420f
    private const val CONTENT_GAP_DP = 6f
    private const val CONTENT_HEIGHT_DP = 60f
    private const val BOTTOM_PADDING_DP = 12f

    /** [circle] is the island's circle around the camera; its diameter is also the height of the pill. */
    fun calculate(screenWidth: Int, circle: CameraHole, density: Float): IslandGeometry {
        fun dp(value: Float) = (value * density).toInt()

        val circleSize = circle.diameter
        val circleTop = circle.centerY - circleSize / 2
        val cardTop = circleTop.coerceAtLeast(0)

        val margin = dp(SIDE_MARGIN_DP)
        val cardWidth = minOf(screenWidth - 2 * margin, dp(MAX_WIDTH_DP))
        val cardLeft = (circle.centerX - cardWidth / 2).coerceIn(margin, screenWidth - margin - cardWidth)

        val contentTop = circleTop + circleSize + dp(CONTENT_GAP_DP) - cardTop
        val contentHeight = dp(CONTENT_HEIGHT_DP)
        val cardHeight = contentTop + contentHeight + dp(BOTTOM_PADDING_DP)

        val centerX = circle.centerX.coerceIn(0, screenWidth)
        val circleLeft = centerX - circleSize / 2

        return IslandGeometry(
            windowWidth = screenWidth,
            windowHeight = cardTop + cardHeight,
            collapsed = PxRect(centerX, circle.centerY, centerX, circle.centerY),
            circle = PxRect(circleLeft, circleTop, circleLeft + circleSize, circleTop + circleSize),
            expanded = PxRect(cardLeft, cardTop, cardLeft + cardWidth, cardTop + cardHeight),
            contentTop = contentTop,
            contentHeight = contentHeight,
        )
    }
}
