package net.solvetheriddle.gameoflife.engine

interface WorldViewConfig {
    companion object {
        const val CELL_SIZE = 5 // Cell square size in pixels
        const val WORLD_SIZE_MIN = 10 // Number of cells on the smaller dimension
        const val WORLD_SIZE_DEFAULT = 500
        const val WRAP_EDGES_DEFAULT = true
        const val AUTO_ZOOM_AMOUNT = 0.2f // Factor of automatic zoom step
        const val GRID_LINE_WIDTH = 2 // Grid line width in pixels
        const val GRID_VISIBLE_DEFAULT = false
        const val ZOOM_DEFAULT = 0.1F
    }

}
