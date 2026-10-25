package io.github.rim95dev.island.overlay

import kotlin.math.roundToInt

/**
 * How the island grows out of the camera and back in.
 *
 * Both styles start from a dot at the camera center, become a circle around the hole, stretch sideways
 * into a pill and drop down into the card. Collapsing runs the same path backwards.
 */
enum class IslandAnimationStyle {
    /** The pill stretches and drops at the same time. The default style. */
    STYLE_1,

    /** The pill stretches to the card width, pauses, then drops into the card. */
    STYLE_2,
}

/**
 * The timing of the island's path: how long it takes and at which fraction of the path each step happens.
 *
 * [pauseMillis] is how long Style 2 waits as a pill before dropping into the card when it opens. Style 1
 * ignores it.
 */
data class IslandMotion(
    val style: IslandAnimationStyle,
    val pauseMillis: Int = DEFAULT_PAUSE_MILLIS,
) {
    val expandMillis: Int
    val collapseMillis: Int

    /** Fraction of the path after which the notification content fades in. */
    val contentFadeStart: Float

    /** Fraction at which the circle around the hole is complete. */
    val circleEnd: Float

    /** Fraction at which the pill reaches the card width. */
    val stretchEnd: Float

    /** Fraction at which the pill starts dropping into the card. */
    val dropStart: Float

    init {
        when (style) {
            IslandAnimationStyle.STYLE_1 -> {
                expandMillis = 450
                collapseMillis = 380
                contentFadeStart = 0.7f
                circleEnd = 0.2f
                stretchEnd = 1f
                dropStart = 0.45f
            }
            IslandAnimationStyle.STYLE_2 -> {
                val pause = pauseMillis.coerceIn(0, MAX_PAUSE_MILLIS)
                val total = CIRCLE_MILLIS + STRETCH_MILLIS + pause + DROP_MILLIS
                expandMillis = total
                collapseMillis = (total * STYLE_2_COLLAPSE_RATIO).roundToInt()
                contentFadeStart = 1f - CONTENT_FADE_MILLIS.toFloat() / total
                circleEnd = CIRCLE_MILLIS.toFloat() / total
                stretchEnd = (CIRCLE_MILLIS + STRETCH_MILLIS).toFloat() / total
                dropStart = (CIRCLE_MILLIS + STRETCH_MILLIS + pause).toFloat() / total
            }
        }
    }

    companion object {
        const val DEFAULT_PAUSE_MILLIS = 60
        const val MAX_PAUSE_MILLIS = 300

        // Style 2 step lengths when opening.
        private const val CIRCLE_MILLIS = 77
        private const val STRETCH_MILLIS = 266
        private const val DROP_MILLIS = 294
        private const val CONTENT_FADE_MILLIS = 98

        /** Style 2 closes along the same path a little faster than it opens. */
        private const val STYLE_2_COLLAPSE_RATIO = 620f / 700f
    }
}
