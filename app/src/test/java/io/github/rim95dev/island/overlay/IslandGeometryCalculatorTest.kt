package io.github.rim95dev.island.overlay

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class IslandGeometryCalculatorTest {

    private val coverHole = CameraHole(centerX = 624, centerY = 52, diameter = 70)
    private val innerHole = CameraHole(centerX = 1862, centerY = 57, diameter = 78)

    private val cover = IslandGeometryCalculator.calculate(screenWidth = 1248, screenHeight = 1972, circle = coverHole, density = DENSITY)
    private val inner = IslandGeometryCalculator.calculate(screenWidth = 2448, screenHeight = 1848, circle = innerHole, density = DENSITY)

    private val style1 = IslandMotion(IslandAnimationStyle.STYLE_1)
    private val motions = listOf(
        style1,
        IslandMotion(IslandAnimationStyle.STYLE_2),
        IslandMotion(IslandAnimationStyle.STYLE_2, pauseMillis = 0),
        IslandMotion(IslandAnimationStyle.STYLE_2, pauseMillis = IslandMotion.MAX_PAUSE_MILLIS),
    )

    @Test
    fun `island starts as a dot at the camera center`() {
        val dot = cover.boundsAt(0f)

        assertEquals(0, dot.width)
        assertEquals(0, dot.height)
        assertEquals(coverHole.centerX, dot.left)
        assertEquals(coverHole.centerY, dot.top)
    }

    @Test
    fun `every motion starts at the camera and ends as the card`() {
        for (motion in motions) {
            for (geometry in listOf(cover, inner)) {
                assertEquals(geometry.collapsed, geometry.boundsAt(0f, motion))
                assertEquals(geometry.expanded, geometry.boundsAt(1f, motion))
            }
        }
    }

    @Test
    fun `near the camera the island is a circle centered on the hole`() {
        for (geometry in listOf(cover, inner)) {
            val holeX = geometry.collapsed.left
            val holeY = geometry.collapsed.top
            for (step in 1..19) {
                val bounds = geometry.boundsAt(step / 100f)
                assertNear(bounds.width, bounds.height)
                assertNear(holeX, bounds.centerX)
                assertNear(holeY, bounds.centerY)
            }
            val circle = geometry.boundsAt(style1.circleEnd)
            assertNear(geometry.circle.width, circle.width)
            assertNear(geometry.circle.height, circle.height)
        }
    }

    @Test
    fun `island never flattens into a thin line`() {
        for (motion in motions) {
            for (geometry in listOf(cover, inner)) {
                for (step in 0..100) {
                    val bounds = geometry.boundsAt(step / 100f, motion)
                    val minimumHeight = minOf(bounds.width, geometry.circle.height)
                    assertTrue("$motion fraction ${step / 100f}: $bounds", bounds.height >= minimumHeight - 1)
                }
            }
        }
    }

    @Test
    fun `style 2 stretches into a full width pill before it drops`() {
        for (motion in motions.filter { it.style == IslandAnimationStyle.STYLE_2 }) {
            for (geometry in listOf(cover, inner)) {
                for (step in 0..100) {
                    val fraction = step / 100f
                    val bounds = geometry.boundsAt(fraction, motion)
                    if (fraction in motion.circleEnd..motion.dropStart) {
                        assertNear(geometry.circle.height, bounds.height)
                        assertNear(geometry.circle.top, bounds.top)
                    }
                    if (fraction >= motion.stretchEnd) {
                        assertNear(geometry.expanded.width, bounds.width)
                    }
                }
            }
        }
    }

    @Test
    fun `style 2 holds the pill still during the pause`() {
        val motion = IslandMotion(IslandAnimationStyle.STYLE_2)
        for (geometry in listOf(cover, inner)) {
            val pause = geometry.boundsAt(motion.stretchEnd, motion)
            for (step in 0..10) {
                val fraction = motion.stretchEnd + (motion.dropStart - motion.stretchEnd) * step / 10f
                assertEquals(pause, geometry.boundsAt(fraction, motion))
            }
            assertNear(geometry.expanded.left, pause.left)
            assertNear(geometry.expanded.right, pause.right)
        }
    }

    @Test
    fun `circle and pill use the given circle size exactly`() {
        for ((geometry, circle) in listOf(cover to coverHole, inner to innerHole)) {
            assertEquals(circle.diameter, geometry.circle.width)
            assertEquals(circle.diameter, geometry.circle.height)
            for (motion in motions) {
                val pill = geometry.boundsAt((motion.circleEnd + motion.dropStart) / 2f, motion)
                assertNear(circle.diameter, pill.height)
            }
        }
    }

    @Test
    fun `inner screen card shifts left to stay on screen and still covers the hole`() {
        assertTrue(inner.expanded.right <= 2448)
        assertEquals(innerHole.centerX, inner.collapsed.left)
        assertTrue(inner.expanded.left < innerHole.centerX && innerHole.centerX < inner.expanded.right)
    }

    @Test
    fun `everything is in screen coordinates of a full width window`() {
        assertEquals(1248, cover.windowWidth)
        assertEquals(coverHole.centerX, cover.collapsed.left)
        assertNear(coverHole.centerX, cover.circle.centerX)
        assertNear(coverHole.centerX, cover.expanded.centerX)
    }

    @Test
    fun `content starts below the hole`() {
        for ((geometry, hole) in listOf(cover to coverHole, inner to innerHole)) {
            assertTrue(geometry.content.top > hole.centerY + hole.diameter / 2)
            assertTrue(geometry.expanded.top <= hole.centerY - hole.diameter / 2)
        }
    }

    @Test
    fun `island fits inside its window`() {
        for (geometry in listOf(cover, inner)) {
            assertTrue(geometry.expanded.left >= 0 && geometry.expanded.right <= geometry.windowWidth)
            assertEquals(geometry.windowHeight, geometry.expanded.bottom)
        }
    }

    private fun assertNear(expected: Int, actual: Int) {
        assertTrue("expected $expected but was $actual", abs(expected - actual) <= 1)
    }

    private companion object {
        const val DENSITY = 2.625f
    }
}
