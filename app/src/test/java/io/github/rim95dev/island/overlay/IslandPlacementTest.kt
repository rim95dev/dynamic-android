package io.github.rim95dev.island.overlay

import org.junit.Assert.assertEquals
import org.junit.Test

class IslandPlacementTest {

    private val topCamera = CameraHole(centerX = 1862, centerY = 57, diameter = 98)
    private val leftCamera = CameraHole(centerX = 52, centerY = 624, diameter = 90)

    @Test
    fun `follow the camera keeps the camera position on every edge`() {
        assertEquals(topCamera, IslandPlacement.FOLLOW_CAMERA.place(topCamera, 2448, 1848, DENSITY))
        assertEquals(leftCamera, IslandPlacement.FOLLOW_CAMERA.place(leftCamera, 1972, 1248, DENSITY))
    }

    @Test
    fun `top only still grows out of a camera at the top`() {
        assertEquals(topCamera, IslandPlacement.TOP_ONLY.place(topCamera, 2448, 1848, DENSITY))
    }

    @Test
    fun `top only opens from the top center when the camera is on another edge`() {
        val placed = IslandPlacement.TOP_ONLY.place(leftCamera, 1972, 1248, DENSITY)

        assertEquals(986, placed.centerX)
        assertEquals(leftCamera.diameter, placed.diameter)
        assertEquals(CameraEdge.TOP, CameraEdge.nearest(placed.centerX, placed.centerY, 1972, 1248))
        assertEquals(leftCamera.diameter / 2 + (6 * DENSITY).toInt(), placed.centerY)
    }

    @Test
    fun `top only opens every fold orientation from the top`() {
        // Cover and main screen in their four rotations, with the camera holes measured on the device.
        val cases = listOf(
            Triple(1248, 1972, CameraHole(624, 52, 90)),
            Triple(1972, 1248, CameraHole(52, 624, 90)),
            Triple(1248, 1972, CameraHole(624, 1920, 90)),
            Triple(1972, 1248, CameraHole(1920, 624, 90)),
            Triple(2448, 1848, CameraHole(1862, 57, 98)),
            Triple(1848, 2448, CameraHole(57, 586, 98)),
            Triple(2448, 1848, CameraHole(586, 1791, 98)),
            Triple(1848, 2448, CameraHole(1791, 1862, 98)),
        )
        for ((width, height, camera) in cases) {
            val placed = IslandPlacement.TOP_ONLY.place(camera, width, height, DENSITY)
            val geometry = IslandGeometryCalculator.calculate(width, height, placed, DENSITY)
            assertEquals("$width x $height $camera", CameraEdge.TOP, geometry.edge)
        }
    }

    private companion object {
        const val DENSITY = 2.15f
    }
}
