package io.github.rim95dev.island.overlay

/** Where the island opens on screen. */
enum class IslandPlacement {
    /** The island grows out of the camera on whichever edge it is, in every orientation. The default. */
    FOLLOW_CAMERA,

    /**
     * The island always opens from the top of the screen. It still grows out of the camera when the camera
     * is at the top, and opens from the top center when the phone is turned so the camera is on another edge.
     */
    TOP_ONLY,
    ;

    /** Where the island's [circle] goes on a [width] × [height] screen under this placement. */
    fun place(circle: CameraHole, width: Int, height: Int, density: Float): CameraHole = when (this) {
        FOLLOW_CAMERA -> circle
        TOP_ONLY -> if (CameraEdge.nearest(circle.centerX, circle.centerY, width, height) == CameraEdge.TOP) {
            circle
        } else {
            circle.copy(centerX = width / 2, centerY = circle.diameter / 2 + (TOP_MARGIN_DP * density).toInt())
        }
    }

    private companion object {
        const val TOP_MARGIN_DP = 6f
    }
}
