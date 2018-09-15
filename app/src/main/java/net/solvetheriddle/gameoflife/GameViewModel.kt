package net.solvetheriddle.gameoflife

import androidx.lifecycle.ViewModel
import java.util.*

class GameViewModel : ViewModel() {

    val gameLiveData = Game()

    fun initGame(verticalCellCount: Int, horizontalCellCount: Int): Array<IntArray> {
        val initState = getRandomState(
                verticalCellCount,
                horizontalCellCount)
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
}
