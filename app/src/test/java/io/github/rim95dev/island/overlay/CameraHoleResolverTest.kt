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
            systemCutoutTop = null,
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
            systemCutoutTop = PxRect(589, 0, 659, 104),
            density = DENSITY,
        )

        assertEquals(CameraHole(centerX = 624, centerY = 52, diameter = 70), hole)
    }

    @Test
    fun `falls back to the top center when the hole is not on the top edge`() {
        val hole = CameraHoleResolver.resolve(
            model = "SM-F971N",
            naturalWidth = 2448,
            naturalHeight = 1848,
            rotation = Surface.ROTATION_90,
            screenWidth = 1848,
            screenHeight = 2448,
            systemCutoutTop = null,
            density = DENSITY,
        )

        assertEquals(CameraHole(centerX = 924, centerY = 52, diameter = 63), hole)
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
            systemCutoutTop = null,
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
