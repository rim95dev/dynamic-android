package io.github.rim95dev.island.overlay

import android.view.Surface
import io.github.rim95dev.island.settings.CameraCalibration
import org.junit.Assert.assertEquals
import org.junit.Test

class ScreenCameraTest {

    private val cover = ScreenCamera(
        naturalWidth = 1248,
        naturalHeight = 1972,
        rotation = Surface.ROTATION_0,
        width = 1248,
        height = 1972,
        density = 2.15f,
        detectedHole = CameraHole(centerX = 624, centerY = 52, diameter = 70),
    )

    @Test
    fun `names the screen by its natural size so every rotation shares one position`() {
        assertEquals("1248x1972", cover.key)
        assertEquals(cover.key, cover.copy(rotation = Surface.ROTATION_90, width = 1972, height = 1248).key)
    }

    @Test
    fun `uses the detected hole with a thin ring until adjusted`() {
        assertEquals(CameraHole(centerX = 624, centerY = 52, diameter = 70 + 2 * 10), cover.circleFor(null))
    }

    @Test
    fun `uses the adjusted position and circle size`() {
        val circle = cover.circleFor(CameraCalibration(x = 620, y = 62, diameter = 48))

        assertEquals(CameraHole(centerX = 620, centerY = 62, diameter = 48), circle)
    }

    @Test
    fun `keeps the default circle size when only the position was adjusted`() {
        val circle = cover.circleFor(CameraCalibration(x = 624, y = 62))

        assertEquals(CameraHole(centerX = 624, centerY = 62, diameter = cover.defaultCircleDiameter), circle)
    }

    @Test
    fun `an adjusted position follows the screen when it rotates`() {
        val landscape = cover.copy(rotation = Surface.ROTATION_90, width = 1972, height = 1248)

        val circle = landscape.circleFor(CameraCalibration(x = 624, y = 61, diameter = 68))

        assertEquals(CameraHole(centerX = 61, centerY = 624, diameter = 68), circle)
    }

    @Test
    fun `a position set in landscape is saved in natural coordinates`() {
        val landscape = cover.copy(rotation = Surface.ROTATION_270, width = 1972, height = 1248)

        val calibration = landscape.toCalibration(x = 1911, y = 624, diameter = 68)

        assertEquals(CameraCalibration(x = 624, y = 61, diameter = 68), calibration)
        assertEquals(CameraHole(centerX = 1911, centerY = 624, diameter = 68), landscape.circleFor(calibration))
    }
}
