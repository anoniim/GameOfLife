package net.solvetheriddle.gameoflife

import android.graphics.Point
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import net.solvetheriddle.gameoflife.view.WorldConfig
import java.util.*
import kotlin.concurrent.thread

class GameViewModel : ViewModel(), Observer {

    private val tag = this::class.java.simpleName

    private val game = Game()
    val worldState = MutableLiveData<WorldState>()
    val gameSpeed = MutableLiveData<Int>()

    /**
     * Initializes Game with a world that fits in the given View
     */
    fun initGame(displaySize: Point) {
        game.new(Game.Settings(
                displaySize.x / WorldConfig.CELL_SIZE,
                displaySize.y / WorldConfig.CELL_SIZE))

        game.worldStateObserver = { worldState.postValue(it) }
        game.getSettings().gameSpeed.addObserver(this)
    }

//    fun initGame() {
//        game.new()
//    }

    fun togglePlay() {
        thread { game.togglePlay() }
    }

    fun cycleSpeed() {
        game.getSettings().cycleSpeed()
    }

    fun cycleZoom() {

    }

    fun onSingleTap() {

    }

    fun onDoubleTap() {

    }

    override fun update(observable: Observable?, value: Any?) {
        when (observable) {
            is GameSpeed -> {
                if(value is Int) gameSpeed.postValue(value)
            }
            else -> Log.w(tag, "unknown observable: ${observable?.javaClass?.simpleName}")
        }
    }
}

/*
        engine takes settings, initializes default state
        to set a new state, create a new engine with (new) or without settings (default will be used)

        Game has LiveData of state, watches the state of engine and delegates to live data
        Game has LiveData or observable of everything that's needed

        Game.new creates a new instance of engine and makes sure the link to live data is kept
*/