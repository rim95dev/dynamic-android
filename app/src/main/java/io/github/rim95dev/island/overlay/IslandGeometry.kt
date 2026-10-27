package io.github.rim95dev.island.overlay

import kotlin.math.roundToInt

/** The screen edge the front camera sits on in the current orientation. */
enum class CameraEdge { TOP, BOTTOM, LEFT, RIGHT }

/**
 * Where the island window sits on screen and how the island grows inside it.
 *
 * The island grows from a dot at the center of the camera hole ([collapsed]) into a circle around the hole
 * ([circle]), stretches sideways into a pill, and then opens into a card ([expanded]) that keeps the hole at
 * its [edge] side. [IslandMotion] decides when each step happens. Collapsing runs the same path backwards,
 * so the island is pulled back into the camera instead of flattening into a line. The notification is laid
 * out in [content].
 *
 * The window always starts at the top-left corner of the screen and spans its width, so all rects are in
 * screen coordinates. Keeping the window still matters: the system animates window moves, which would make
 * the island slide in from the side instead of growing out of the camera.
 */
data class IslandGeometry(
    val edge: CameraEdge,
    val windowWidth: Int,
    val windowHeight: Int,
    val collapsed: PxRect,
    val circle: PxRect,
    val expanded: PxRect,
    val content: PxRect,
) {
    /**
     * The island bounds at [fraction] of the way from [collapsed] (0) to [expanded] (1) along the [motion] path.
     *
     * Each side of the circle moves to the matching side of the card: the left and right sides while the pill
     * stretches, the top and bottom sides while it opens into the card. Because the card is placed against the
     * camera's edge, the island always grows away from the camera into the screen.
     */
    fun boundsAt(fraction: Float, motion: IslandMotion = IslandMotion(IslandAnimationStyle.STYLE_1)): PxRect {
        val f = fraction.coerceIn(0f, 1f)
        if (f < motion.circleEnd) {
            val size = circle.width * (f / motion.circleEnd)
            val centerX = collapsed.left.toFloat()
            val centerY = collapsed.top.toFloat()
            return PxRect(
                left = (centerX - size / 2f).roundToInt(),
                top = (centerY - size / 2f).roundToInt(),
                right = (centerX + size / 2f).roundToInt(),
                bottom = (centerY + size / 2f).roundToInt(),
            )
        }

        val stretch = smoothPhase(f, motion.circleEnd, motion.stretchEnd)
        val open = smoothPhase(f, motion.dropStart, 1f)
        return PxRect(
            left = lerp(circle.left, expanded.left, stretch),
            top = lerp(circle.top, expanded.top, open),
            right = lerp(circle.right, expanded.right, stretch),
            bottom = lerp(circle.bottom, expanded.bottom, open),
        )
    }

    private fun smoothPhase(fraction: Float, start: Float, end: Float): Float {
        val t = ((fraction - start) / (end - start)).coerceIn(0f, 1f)
        return t * t * (3f - 2f * t)
    }

    private fun lerp(start: Int, stop: Int, fraction: Float): Int = (start + (stop - start) * fraction).roundToInt()
}

object IslandGeometryCalculator {

    private const val SIDE_MARGIN_DP = 12f
    private const val MAX_WIDTH_DP = 420f
    private const val CONTENT_GAP_DP = 6f
    private const val CONTENT_HEIGHT_DP = 60f
    private const val CARD_PADDING_DP = 12f
    private const val CONTENT_SIDE_PADDING_DP = 18f
    private const val CONTENT_BESIDE_HOLE_GAP_DP = 10f

    /**
     * [circle] is the island's circle around the camera; its diameter is also the height of the pill.
     * The card opens away from the screen edge nearest to the camera.
     */
    fun calculate(screenWidth: Int, screenHeight: Int, circle: CameraHole, density: Float): IslandGeometry {
        fun dp(value: Float) = (value * density).toInt()

        val size = circle.diameter
        val centerX = circle.centerX.coerceIn(0, screenWidth)
        val centerY = circle.centerY.coerceIn(0, screenHeight)
        val circleLeft = centerX - size / 2
        val circleTop = centerY - size / 2
        val circleRect = PxRect(circleLeft, circleTop, circleLeft + size, circleTop + size)
        val margin = dp(SIDE_MARGIN_DP)
        val maxWidth = minOf(screenWidth - 2 * margin, dp(MAX_WIDTH_DP))
        val contentHeight = dp(CONTENT_HEIGHT_DP)
        val edge = nearestEdge(centerX, centerY, screenWidth, screenHeight)

        val expanded: PxRect
        val content: PxRect
        when (edge) {
            CameraEdge.TOP, CameraEdge.BOTTOM -> {
                val left = (centerX - maxWidth / 2).coerceIn(margin, screenWidth - margin - maxWidth)
                val height = size + dp(CONTENT_GAP_DP) + contentHeight + dp(CARD_PADDING_DP)
                val top = if (edge == CameraEdge.TOP) {
                    circleRect.top.coerceAtLeast(0)
                } else {
                    circleRect.bottom.coerceAtMost(screenHeight) - height
                }
                expanded = PxRect(left, top, left + maxWidth, top + height)
                val contentTop = if (edge == CameraEdge.TOP) {
                    circleRect.bottom + dp(CONTENT_GAP_DP)
                } else {
                    circleRect.top - dp(CONTENT_GAP_DP) - contentHeight
                }
                content = PxRect(
                    left = expanded.left + dp(CONTENT_SIDE_PADDING_DP),
                    top = contentTop,
                    right = expanded.right - dp(CONTENT_SIDE_PADDING_DP),
                    bottom = contentTop + contentHeight,
                )
            }
            CameraEdge.LEFT, CameraEdge.RIGHT -> {
                val height = maxOf(size, contentHeight) + 2 * dp(CARD_PADDING_DP)
                val top = (centerY - height / 2).coerceIn(0, screenHeight - height)
                val contentTop = centerY - contentHeight / 2
                if (edge == CameraEdge.LEFT) {
                    val left = circleRect.left.coerceAtLeast(0)
                    val right = minOf(left + maxWidth, screenWidth - margin)
                    expanded = PxRect(left, top, right, top + height)
                    content = PxRect(
                        left = circleRect.right + dp(CONTENT_BESIDE_HOLE_GAP_DP),
                        top = contentTop,
                        right = expanded.right - dp(CONTENT_SIDE_PADDING_DP),
                        bottom = contentTop + contentHeight,
                    )
                } else {
                    val right = circleRect.right.coerceAtMost(screenWidth)
                    val left = maxOf(right - maxWidth, margin)
                    expanded = PxRect(left, top, right, top + height)
                    content = PxRect(
                        left = expanded.left + dp(CONTENT_SIDE_PADDING_DP),
                        top = contentTop,
                        right = circleRect.left - dp(CONTENT_BESIDE_HOLE_GAP_DP),
                        bottom = contentTop + contentHeight,
                    )
                }
            }
        }

        return IslandGeometry(
            edge = edge,
            windowWidth = screenWidth,
            windowHeight = maxOf(expanded.bottom, circleRect.bottom),
            collapsed = PxRect(centerX, centerY, centerX, centerY),
            circle = circleRect,
            expanded = expanded,
            content = content,
        )
    }

    private fun nearestEdge(x: Int, y: Int, width: Int, height: Int): CameraEdge =
        listOf(
            CameraEdge.TOP to y,
            CameraEdge.BOTTOM to height - y,
            CameraEdge.LEFT to x,
            CameraEdge.RIGHT to width - x,
        ).minBy { it.second }.first
}
