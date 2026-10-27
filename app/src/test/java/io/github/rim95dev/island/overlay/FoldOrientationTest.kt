package io.github.rim95dev.island.overlay

import android.view.Surface
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Every screen and rotation of the Galaxy Z Fold8, with the camera holes measured on the device. */
class FoldOrientationTest {

    private data class Case(
        val name: String,
        val naturalWidth: Int,
        val naturalHeight: Int,
        val rotation: Int,
        val systemCutoutNatural: PxRect?,
        val expectedEdge: CameraEdge,
    ) {
        val rotated = rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270
        val width = if (rotated) naturalHeight else naturalWidth
        val height = if (rotated) naturalWidth else naturalHeight
    }

    private val coverCutout = PxRect(589, 0, 659, 104)

    private val cases = listOf(
        Case("cover portrait", 1248, 1972, Surface.ROTATION_0, coverCutout, CameraEdge.TOP),
        Case("cover landscape, camera left", 1248, 1972, Surface.ROTATION_90, coverCutout, CameraEdge.LEFT),
        Case("cover upside down", 1248, 1972, Surface.ROTATION_180, coverCutout, CameraEdge.BOTTOM),
        Case("cover landscape, camera right", 1248, 1972, Surface.ROTATION_270, coverCutout, CameraEdge.RIGHT),
        Case("main screen, unfolded", 2448, 1848, Surface.ROTATION_0, null, CameraEdge.TOP),
        Case("main screen upright, camera left", 2448, 1848, Surface.ROTATION_90, null, CameraEdge.LEFT),
        Case("main screen upside down", 2448, 1848, Surface.ROTATION_180, null, CameraEdge.BOTTOM),
        Case("main screen upright, camera right", 2448, 1848, Surface.ROTATION_270, null, CameraEdge.RIGHT),
    )

    private val motions = listOf(
        IslandMotion(IslandAnimationStyle.STYLE_1),
        IslandMotion(IslandAnimationStyle.STYLE_2),
    )

    private fun geometryOf(case: Case): Pair<CameraHole, IslandGeometry> {
        val systemCutout = case.systemCutoutNatural
            ?.let { CameraHoleResolver.rotate(it, case.rotation, case.naturalWidth, case.naturalHeight) }
        val hole = CameraHoleResolver.resolve(
            model = "SM-F971N",
            naturalWidth = case.naturalWidth,
            naturalHeight = case.naturalHeight,
            rotation = case.rotation,
            screenWidth = case.width,
            screenHeight = case.height,
            systemCutout = systemCutout,
            density = DENSITY,
        )
        val circle = hole.copy(diameter = hole.diameter + 20)
        return circle to IslandGeometryCalculator.calculate(case.width, case.height, circle, DENSITY)
    }

    @Test
    fun `the island opens away from the edge the camera is on`() {
        for (case in cases) {
            val (_, geometry) = geometryOf(case)
            assertEquals(case.name, case.expectedEdge, geometry.edge)
        }
    }

    @Test
    fun `the island starts and ends at the camera`() {
        for (case in cases) {
            val (circle, geometry) = geometryOf(case)
            for (motion in motions) {
                val dot = geometry.boundsAt(0f, motion)
                assertEquals(case.name, circle.centerX, dot.left)
                assertEquals(case.name, circle.centerY, dot.top)
                assertEquals(case.name, geometry.expanded, geometry.boundsAt(1f, motion))
            }
        }
    }

    @Test
    fun `the card stays on screen and keeps the camera at its edge`() {
        for (case in cases) {
            val (circle, geometry) = geometryOf(case)
            val card = geometry.expanded
            assertTrue(case.name, card.left >= 0 && card.top >= 0 && card.right <= case.width && card.bottom <= case.height)
            assertTrue(case.name, circle.centerX in card.left..card.right && circle.centerY in card.top..card.bottom)
            assertTrue(case.name, geometry.windowHeight >= card.bottom && geometry.windowWidth == case.width)
        }
    }

    @Test
    fun `the notification sits inside the card and clear of the camera`() {
        for (case in cases) {
            val (_, geometry) = geometryOf(case)
            val content = geometry.content
            val card = geometry.expanded
            val hole = geometry.circle
            assertTrue(case.name, content.width > 0 && content.height > 0)
            assertTrue(case.name, content.left >= card.left && content.right <= card.right)
            assertTrue(case.name, content.top >= card.top && content.bottom <= card.bottom)
            val overlapsHole = content.left < hole.right && hole.left < content.right &&
                content.top < hole.bottom && hole.top < content.bottom
            assertTrue(case.name, !overlapsHole)
        }
    }

    @Test
    fun `the island never flattens into a thin line in any orientation`() {
        for (case in cases) {
            val (_, geometry) = geometryOf(case)
            for (motion in motions) {
                for (step in 0..100) {
                    val bounds = geometry.boundsAt(step / 100f, motion)
                    val minimumHeight = minOf(bounds.width, geometry.circle.height)
                    assertTrue("${case.name} ${motion.style} ${step / 100f}", bounds.height >= minimumHeight - 1)
                }
            }
        }
    }

    private companion object {
        const val DENSITY = 2.15f
    }
}
