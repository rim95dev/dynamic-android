package io.github.rim95dev.island.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CameraCalibrationTest {

    @Test
    fun `encodes and decodes a position with a circle size`() {
        val calibration = CameraCalibration(x = 1862, y = 57, diameter = 52)

        assertEquals("1862,57,52", calibration.encode())
        assertEquals(calibration, CameraCalibration.decode(calibration.encode()))
    }

    @Test
    fun `reads positions saved before the circle size existed`() {
        assertEquals(CameraCalibration(x = 624, y = 62, diameter = null), CameraCalibration.decode("624,62"))
    }

    @Test
    fun `rejects malformed values`() {
        assertNull(CameraCalibration.decode(null))
        assertNull(CameraCalibration.decode(""))
        assertNull(CameraCalibration.decode("1862"))
        assertNull(CameraCalibration.decode("1862,abc"))
        assertNull(CameraCalibration.decode("1,2,3,4"))
    }
}
