package net.solvetheriddle.gameoflife

import android.graphics.Point
import androidx.lifecycle.ViewModel
import kotlin.concurrent.thread

class GameViewModel : ViewModel() {

    val gameLiveData = Game(Game.GameSettings())

    /**
     * Initializes Game with a world that fits in the given View
     */
    fun initGame(displaySize: Point): WorldState {
        val initState = WorldFactory.random(
                displaySize.x / WorldConfig.CELL_SIZE,
                displaySize.y / WorldConfig.CELL_SIZE)
        gameLiveData.worldState = initState
        return initState
    }

    fun togglePlay() {
        thread { gameLiveData.togglePlay() }
    }

    fun cycleSpeed() {
        gameLiveData.settings.cycleSpeed()
    }

    fun cycleZoom() {

    }

    fun onSingleTap() {

    }

    fun onDoubleTap() {

    }

    val speedObservable = gameLiveData.settings.gameSpeed
}
