package net.solvetheriddle.gameoflife

import android.os.SystemClock
import android.util.Log
import androidx.annotation.WorkerThread
import androidx.lifecycle.LiveData
import net.solvetheriddle.gameoflife.WorldConfig.Companion.WORLD_SIZE_DEFAULT
import net.solvetheriddle.gameoflife.WorldConfig.Companion.WORLD_SIZE_MIN
import java.util.*

class Game(val settings: GameSettings) : LiveData<WorldState>() {

    private val tag = this::class.java.simpleName

    enum class GameState {
        PAUSED, RUNNING
    }

    interface WorldStateObserver {
        fun onNextTurn(worldState: WorldState)
    }

    private var gameState = GameState.PAUSED
        set(state) {
            field = state
            Log.d(tag, "State = ${state.name}")
        }

    var worldState: WorldState = WorldFactory.empty(settings.getColCount(), settings.getRowCount())
        set(newState) {
            field = newState
            worldStateObserver?.onNextTurn(newState)
            nextState = newState
        }
    private var worldStateObserver: Game.WorldStateObserver? = null
    private lateinit var nextState: WorldState

    @WorkerThread
    fun togglePlay() {
        if (gameState == GameState.RUNNING) pause() else run()
    }

    @WorkerThread
    private fun run() {
        gameState = GameState.RUNNING
        while (gameState == GameState.RUNNING) {
            nextTurn()
            SystemClock.sleep(settings.gameSpeed.millis.toLong())
        }
    }

    private fun pause() {
        gameState = GameState.PAUSED
    }

    override fun onActive() {
        worldStateObserver = object : WorldStateObserver {
            override fun onNextTurn(worldState: WorldState) {
                postValue(worldState)
            }
        }
    }

    override fun onInactive() {
        worldStateObserver = null
    }

    private fun nextTurn() {
        assertWorldValid(worldState)

        nextState = WorldFactory.empty(worldState.size, worldState[0].size)

        for (i in worldState.indices) {
            for (j in 0 until worldState[0].size) {
                val numOfNeighbours = getNumOfNeighbours(worldState, i, j)

                if (isAlive(worldState[i][j])) {
                    nextState[i][j] = if (numOfNeighbours in 2..3) 1 else 0
                } else {
                    nextState[i][j] = if (numOfNeighbours == 3) 1 else 0
                }
            }
        }
        worldState = nextState.copyOf()
    }

    private fun getNumOfNeighbours(state: WorldState, i: Int, j: Int): Int {
        val upperNeighbor = i - 1
        val lowerNeighbor = i + 1
        val leftNeighbor = j - 1
        val rightNeighbor = j + 1
        val isUpperEdge = upperNeighbor < 0
        val isLowerEdge = lowerNeighbor >= state.size
        val isLeftEdge = leftNeighbor < 0
        val isRightEdge = rightNeighbor >= state[0].size

        var numOfNeighbours = 0
        if (!isUpperEdge && !isLeftEdge && isAlive(state[upperNeighbor][leftNeighbor])) {
            numOfNeighbours++
        }
        if (!isUpperEdge && isAlive(state[upperNeighbor][j])) {
            numOfNeighbours++
        }
        if (!isUpperEdge && !isRightEdge && isAlive(state[upperNeighbor][rightNeighbor])) {
            numOfNeighbours++
        }
        if (!isLeftEdge && isAlive(state[i][leftNeighbor])) {
            numOfNeighbours++
        }
        if (!isRightEdge && isAlive(state[i][rightNeighbor])) {
            numOfNeighbours++
        }
        if (!isLowerEdge && !isLeftEdge && isAlive(state[lowerNeighbor][leftNeighbor])) {
            numOfNeighbours++
        }
        if (!isLowerEdge && isAlive(state[lowerNeighbor][j])) {
            numOfNeighbours++
        }
        if (!isLowerEdge && !isRightEdge && isAlive(state[lowerNeighbor][rightNeighbor])) {
            numOfNeighbours++
        }
        return numOfNeighbours
    }

    private fun isAlive(cell: Int): Boolean {
        return cell > 0
    }

    private fun assertWorldValid(worldState: WorldState) {
        if (worldState.size < WORLD_SIZE_MIN || worldState[0].size < WORLD_SIZE_MIN)
            throw IllegalStateException("Size must be at least $WORLD_SIZE_MIN, is "
                    + if (worldState.isEmpty()) "0" else "[${worldState.size}, ${worldState[0].size}]")
    }

    class GameSettings(private var colCount: Int = WORLD_SIZE_DEFAULT,
                       private var rowCount: Int = WORLD_SIZE_DEFAULT,
                       val gameSpeed: GameSpeed = GameSpeed()) {


        fun cycleSpeed() {
            gameSpeed.cycleUp()
        }

        fun getColCount(): Int {
            return colCount
        }

        fun getRowCount(): Int {
            return rowCount
        }
    }
}

typealias WorldState = Array<IntArray>

class WorldFactory {
    companion object {

        fun empty(colCount: Int = 0, rowCount: Int = 0) = Array(colCount) { IntArray(rowCount) }

        fun random(colCount: Int, rowCount: Int): WorldState {
            return Array(colCount) { _ ->
                IntArray(rowCount) {
                    Random().nextInt(2)
                }
            }
        }
    }
}