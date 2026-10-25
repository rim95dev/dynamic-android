package io.github.rim95dev.island.settings

/**
 * The user-adjusted camera hole on one screen, in pixels of that screen.
 *
 * [diameter] is the size of the island's circle around the camera, which is also the height of the pill.
 * It is null when only the position was adjusted, and the detected size is used.
 */
data class CameraCalibration(val x: Int, val y: Int, val diameter: Int? = null) {

    fun encode(): String = if (diameter == null) "$x,$y" else "$x,$y,$diameter"

    companion object {
        fun decode(value: String?): CameraCalibration? {
            val parts = value?.split(',') ?: return null
            if (parts.size !in 2..3) return null
            val numbers = parts.map { it.toIntOrNull() ?: return null }
            return CameraCalibration(x = numbers[0], y = numbers[1], diameter = numbers.getOrNull(2))
        }
    }
}
