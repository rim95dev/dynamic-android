package io.github.rim95dev.island.settings

import io.github.rim95dev.island.overlay.CameraHoleResolver

/**
 * The user-adjusted camera hole on one screen, in pixels of that screen in its natural orientation.
 *
 * [diameter] is the size of the island's circle around the camera, which is also the height of the pill.
 * It is null when only the position was adjusted, and the detected size is used.
 */
data class CameraCalibration(val x: Int, val y: Int, val diameter: Int? = null) {

    fun encode(): String = if (diameter == null) "$x,$y" else "$x,$y,$diameter"

    companion object {
        /**
         * Builds the calibrations keyed by screen from saved `key to calibration` pairs.
         *
         * Older versions saved one position per orientation as `<width>x<height>@<rotation>`, in that
         * orientation's coordinates. Those are moved to natural coordinates under `<width>x<height>`. A position
         * saved in the new form wins, then the one saved in the natural orientation.
         */
        fun fromSaved(saved: List<Pair<String, CameraCalibration>>): Map<String, CameraCalibration> {
            val (current, legacy) = saved.partition { '@' !in it.first }
            val currentKeys = current.map { it.first }.toSet()
            val migrated = legacy
                .mapNotNull { (key, calibration) -> fromLegacy(key, calibration) }
                .sortedByDescending { (_, rotation, _) -> rotation }
                .associate { (screenKey, _, calibration) -> screenKey to calibration }
                .filterKeys { it !in currentKeys }
            return current.toMap() + migrated
        }

        private fun fromLegacy(key: String, calibration: CameraCalibration): Triple<String, Int, CameraCalibration>? {
            val screenKey = key.substringBefore('@')
            val rotation = key.substringAfter('@').toIntOrNull() ?: return null
            val width = screenKey.substringBefore('x').toIntOrNull() ?: return null
            val height = screenKey.substringAfter('x').toIntOrNull() ?: return null
            val (x, y) = CameraHoleResolver.unrotatePoint(calibration.x, calibration.y, rotation, width, height)
            return Triple(screenKey, rotation, calibration.copy(x = x, y = y))
        }

        fun decode(value: String?): CameraCalibration? {
            val parts = value?.split(',') ?: return null
            if (parts.size !in 2..3) return null
            val numbers = parts.map { it.toIntOrNull() ?: return null }
            return CameraCalibration(x = numbers[0], y = numbers[1], diameter = numbers.getOrNull(2))
        }
    }
}
