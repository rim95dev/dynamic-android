package io.github.rim95dev.island.overlay

import android.view.Surface
import org.junit.Assert.assertEquals
import org.junit.Test

class CameraHoleResolverTest {

    @Test
    fun `fold8 inner screen uses the measured hole`() {
        val hole = CameraHoleResolver.resolve(
            model = "SM-F971N",
            naturalWidth = 2448,
            naturalHeight = 1848,
            rotation = Surface.ROTATION_0,
            screenWidth = 2448,
            screenHeight = 1848,
            systemCutout = null,
            density = DENSITY,
        )

        assertEquals(CameraHole(centerX = 1862, centerY = 57, diameter = 78), hole)
    }

    @Test
    fun `fold8 cover screen uses the system cutout`() {
        val hole = CameraHoleResolver.resolve(
            model = "SM-F971N",
            naturalWidth = 1248,
            naturalHeight = 1972,
            rotation = Surface.ROTATION_0,
            screenWidth = 1248,
            screenHeight = 1972,
            systemCutout = PxRect(589, 0, 659, 104),
            density = DENSITY,
        )

        assertEquals(CameraHole(centerX = 624, centerY = 52, diameter = 70), hole)
    }

    @Test
    fun `fold8 main screen held upright puts the camera on the side edge`() {
        val hole = CameraHoleResolver.resolve(
            model = "SM-F971N",
            naturalWidth = 2448,
            naturalHeight = 1848,
            rotation = Surface.ROTATION_90,
            screenWidth = 1848,
            screenHeight = 2448,
            systemCutout = null,
            density = DENSITY,
        )

        assertEquals(CameraHole(centerX = 57, centerY = 586, diameter = 78), hole)
    }

    @Test
    fun `cover screen in landscape uses the side cutout`() {
        val hole = CameraHoleResolver.resolve(
            model = "SM-F971N",
            naturalWidth = 1248,
            naturalHeight = 1972,
            rotation = Surface.ROTATION_90,
            screenWidth = 1972,
            screenHeight = 1248,
            systemCutout = PxRect(0, 589, 104, 659),
            density = DENSITY,
        )

        assertEquals(CameraHole(centerX = 52, centerY = 624, diameter = 70), hole)
    }

    @Test
    fun `points rotate and come back to where they were`() {
        for (rotation in listOf(Surface.ROTATION_0, Surface.ROTATION_90, Surface.ROTATION_180, Surface.ROTATION_270)) {
            val (x, y) = CameraHoleResolver.rotatePoint(1862, 57, rotation, 2448, 1848)
            assertEquals(1862 to 57, CameraHoleResolver.unrotatePoint(x, y, rotation, 2448, 1848))
        }
        assertEquals(57 to 586, CameraHoleResolver.rotatePoint(1862, 57, Surface.ROTATION_90, 2448, 1848))
    }

    @Test
    fun `falls back to the top center on unknown devices without a cutout`() {
        val hole = CameraHoleResolver.resolve(
            model = "Pixel 10",
            naturalWidth = 1080,
            naturalHeight = 2400,
            rotation = Surface.ROTATION_0,
            screenWidth = 1080,
            screenHeight = 2400,
            systemCutout = null,
            density = DENSITY,
        )

        assertEquals(540, hole.centerX)
    }

    @Test
    fun `rotates a rect like the platform`() {
        val rect = PxRect(1823, 18, 1901, 96)

        assertEquals(PxRect(18, 547, 96, 625), CameraHoleResolver.rotate(rect, Surface.ROTATION_90, 2448, 1848))
        assertEquals(PxRect(547, 1752, 625, 1830), CameraHoleResolver.rotate(rect, Surface.ROTATION_180, 2448, 1848))
        assertEquals(PxRect(1752, 1823, 1830, 1901), CameraHoleResolver.rotate(rect, Surface.ROTATION_270, 2448, 1848))
    }

    private companion object {
        const val DENSITY = 2.625f
    }
}
