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

    @Test
    fun `keeps a position saved for the natural orientation of the cover screen`() {
        val saved = listOf("1248x1972@0" to CameraCalibration(x = 624, y = 61, diameter = 68))

        assertEquals(mapOf("1248x1972" to CameraCalibration(x = 624, y = 61, diameter = 68)), CameraCalibration.fromSaved(saved))
    }

    @Test
    fun `moves a position saved in landscape to natural coordinates`() {
        val saved = listOf("1248x1972@1" to CameraCalibration(x = 61, y = 624, diameter = 68))

        assertEquals(mapOf("1248x1972" to CameraCalibration(x = 624, y = 61, diameter = 68)), CameraCalibration.fromSaved(saved))
    }

    @Test
    fun `prefers the new form, then the natural orientation`() {
        val newForm = CameraCalibration(x = 600, y = 60, diameter = 60)
        val natural = CameraCalibration(x = 624, y = 61, diameter = 68)

        assertEquals(
            natural,
            CameraCalibration.fromSaved(
                listOf("1248x1972@1" to CameraCalibration(x = 70, y = 600), "1248x1972@0" to natural),
            )["1248x1972"],
        )
        assertEquals(
            newForm,
            CameraCalibration.fromSaved(listOf("1248x1972@0" to natural, "1248x1972" to newForm))["1248x1972"],
        )
    }
}
