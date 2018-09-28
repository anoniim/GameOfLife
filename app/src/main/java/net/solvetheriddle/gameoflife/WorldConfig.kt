package net.solvetheriddle.gameoflife

interface WorldConfig {
    companion object {
        const val LINE_WIDTH = 2 // Grid line width in pixels
        const val MIN_CELL_SIZE = 5 // Cell square size in pixels
        const val MIN_WORLD_SIZE = 10 // Number of cells on the smaller dimension
        const val AUTO_ZOOM_AMOUNT = 0.2f // Factor of automatic zoom step
    }

}
