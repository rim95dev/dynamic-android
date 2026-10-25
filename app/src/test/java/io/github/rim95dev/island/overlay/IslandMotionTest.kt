package io.github.rim95dev.island.overlay

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class IslandMotionTest {

    @Test
    fun `style 2 pauses for the chosen time when it opens`() {
        for (pause in listOf(0, 30, 60, 150, 300)) {
            val motion = IslandMotion(IslandAnimationStyle.STYLE_2, pauseMillis = pause)
            val pauseOnScreen = (motion.dropStart - motion.stretchEnd) * motion.expandMillis

            assertEquals(pause.toFloat(), pauseOnScreen, 0.5f)
        }
    }

    @Test
    fun `changing the pause keeps the moving steps at the same speed`() {
        val short = IslandMotion(IslandAnimationStyle.STYLE_2, pauseMillis = 0)
        val long = IslandMotion(IslandAnimationStyle.STYLE_2, pauseMillis = 300)

        assertEquals(short.stretchEnd * short.expandMillis, long.stretchEnd * long.expandMillis, 0.5f)
        assertEquals(
            (1f - short.dropStart) * short.expandMillis,
            (1f - long.dropStart) * long.expandMillis,
            0.5f,
        )
        assertEquals(300, long.expandMillis - short.expandMillis)
    }

    @Test
    fun `style 2 closes a little faster than it opens`() {
        val motion = IslandMotion(IslandAnimationStyle.STYLE_2)

        assertTrue(motion.collapseMillis < motion.expandMillis)
    }

    @Test
    fun `pause is kept within the allowed range`() {
        val negative = IslandMotion(IslandAnimationStyle.STYLE_2, pauseMillis = -50)
        val tooLong = IslandMotion(IslandAnimationStyle.STYLE_2, pauseMillis = 5_000)

        assertEquals(negative.stretchEnd, negative.dropStart, 0.0001f)
        assertEquals(
            IslandMotion.MAX_PAUSE_MILLIS.toFloat(),
            (tooLong.dropStart - tooLong.stretchEnd) * tooLong.expandMillis,
            0.5f,
        )
    }

    @Test
    fun `style 1 ignores the pause`() {
        assertEquals(
            IslandMotion(IslandAnimationStyle.STYLE_1, pauseMillis = 0),
            IslandMotion(IslandAnimationStyle.STYLE_1, pauseMillis = 0).copy(),
        )
        val withPause = IslandMotion(IslandAnimationStyle.STYLE_1, pauseMillis = 300)
        assertEquals(450, withPause.expandMillis)
        assertEquals(0.45f, withPause.dropStart, 0.0001f)
    }
}
