package io.github.rim95dev.island.overlay

import io.github.rim95dev.island.settings.CameraCalibration
import org.junit.Assert.assertEquals
import org.junit.Test

class ScreenCameraTest {

    private val screen = ScreenCamera(
        key = "1248x1972@0",
        width = 1248,
        height = 1972,
        density = 2.15f,
        detectedHole = CameraHole(centerX = 624, centerY = 52, diameter = 70),
    )

    @Test
    fun `uses the detected hole with a thin ring until adjusted`() {
        assertEquals(CameraHole(centerX = 624, centerY = 52, diameter = 70 + 2 * 10), screen.circleFor(null))
    }

    @Test
    fun `uses the adjusted position and circle size`() {
        val circle = screen.circleFor(CameraCalibration(x = 620, y = 62, diameter = 48))

        assertEquals(CameraHole(centerX = 620, centerY = 62, diameter = 48), circle)
    }

    @Test
    fun `keeps the default circle size when only the position was adjusted`() {
        val circle = screen.circleFor(CameraCalibration(x = 624, y = 62))

        assertEquals(CameraHole(centerX = 624, centerY = 62, diameter = screen.defaultCircleDiameter), circle)
    }
}
