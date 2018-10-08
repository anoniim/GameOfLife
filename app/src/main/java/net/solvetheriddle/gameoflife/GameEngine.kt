package net.solvetheriddle.gameoflife

import android.os.SystemClock
import android.util.Log
import androidx.annotation.WorkerThread
import net.solvetheriddle.gameoflife.view.WorldConfig
import kotlin.properties.Delegates

class GameEngine(
        val settings: Game.Settings,
        worldStateObserver: WorldStateObserver
) {

    private val tag = this::class.java.simpleName

    private var worldState: WorldState
            by Delegates.observable(settings.getInitState(), worldStateObserver)
    private lateinit var nextState: WorldState

    private var gameState = Game.GameState.PAUSED
        set(state) {
            field = state
            Log.d(tag, "Game State = ${state.name}")
        }

    @WorkerThread
    fun togglePlay() {
        if (gameState == Game.GameState.RUNNING) pause() else run()
    }

    @WorkerThread
    private fun run() {
        gameState = Game.GameState.RUNNING
        while (gameState == Game.GameState.RUNNING) {
            nextTurn()
            SystemClock.sleep(settings.gameSpeed.millis.toLong())
        }
    }

    private fun pause() {
        gameState = Game.GameState.PAUSED
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
        // FIXME reversed dimensions
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
        if (worldState.size < WorldConfig.WORLD_SIZE_MIN || worldState[0].size < WorldConfig.WORLD_SIZE_MIN)
            throw IllegalStateException("Size must be at least ${WorldConfig.WORLD_SIZE_MIN}, is "
                    + if (worldState.isEmpty()) "0" else "[${worldState.size}, ${worldState[0].size}]")
    }
}
