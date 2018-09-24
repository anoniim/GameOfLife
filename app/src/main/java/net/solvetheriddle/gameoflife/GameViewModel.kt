package net.solvetheriddle.gameoflife

import android.graphics.Point
import androidx.lifecycle.ViewModel
import java.util.*

class GameViewModel : ViewModel() {

    val gameLiveData = Game()

    /**
     * Initializes Game with a world that fits in the given View
     */
    fun initGame(displaySize: Point): Array<IntArray> {
        val initState = getRandomState(
                displaySize.x / WorldConfig.MIN_CELL_SIZE,
                displaySize.y / WorldConfig.MIN_CELL_SIZE)
        gameLiveData.worldState = initState
        return initState
    }

    private fun getRandomState(verticalCellCount: Int, horizontalCellCount: Int): Array<IntArray> {
        return Array(verticalCellCount) {
            IntArray(horizontalCellCount) {
                Random().nextInt(2)
            }
        }
    }

    fun togglePlay() {
        gameLiveData.togglePlay()
    }

    fun cycleSpeed() {
        gameLiveData.cycleSpeed()
    }

    fun cycleZoom() {

    }

    fun onSingleTap() {

    }

    fun onDoubleTap() {

    }
}
