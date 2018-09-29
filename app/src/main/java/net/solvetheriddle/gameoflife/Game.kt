package net.solvetheriddle.gameoflife

import android.os.SystemClock
import android.util.Log
import androidx.lifecycle.LiveData

class Game(val settings: GameSettings)
    : LiveData<Array<IntArray>>() {

    private val tag = this::class.java.simpleName

    companion object {
        private const val MIN_SIZE = 5
        private const val DEFAULT_SIZE = 500
    }

    enum class GameState {
        PAUSED, RUNNING
    }

    interface WorldStateObserver {
        fun onNextTurn(worldState: Array<IntArray>)
    }

    private var gameState = GameState.PAUSED
        set(state) {
            field = state
            Log.d(tag, "State = ${state.name}")
        }

    var worldState: Array<IntArray> = emptyWorldState(settings.getVerticalCellCount(), settings.getHorizontalCellCount())
        set(newState) {
            field = newState
            worldStateObserver?.onNextTurn(newState)
        }
    private var worldStateObserver: Game.WorldStateObserver? = null

    fun togglePlay() {
        if (gameState == GameState.RUNNING) pause() else run()
    }

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
            override fun onNextTurn(worldState: Array<IntArray>) {
                Log.d(tag, "Next turn posted")
                postValue(worldState)
            }
        }
    }

    override fun onInactive() {
        worldStateObserver = null
    }

    private fun nextTurn() {
//        assertWorldInitialized()
        assertWorldValid(worldState)

        val nextState = emptyWorldState(worldState.size, worldState[0].size)

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
        worldState = nextState
    }

    private fun getNumOfNeighbours(state: Array<IntArray>, i: Int, j: Int): Int {
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

    private fun emptyWorldState(minVerticalCellCount: Int, minHorizontalCellCount: Int): Array<IntArray> {
        return Array(minVerticalCellCount) {
            IntArray(minHorizontalCellCount)
        }
    }

//    private fun assertWorldInitialized() {
//        if (!this::worldState.isInitialized)
//            throw IllegalStateException("World has not been initialized")
//    }

    private fun assertWorldValid(worldState: Array<IntArray>) {
        if (worldState.size < MIN_SIZE || worldState[0].size < MIN_SIZE)
            throw IllegalStateException("Size must be at least $MIN_SIZE, is "
                    + if (worldState.isEmpty()) "0" else "[${worldState.size}, ${worldState[0].size}]")
    }

    class GameSettings(var minVerticalCellCount: Int = DEFAULT_SIZE,
                       var minHorizontalCellCount: Int = DEFAULT_SIZE,
                       val gameSpeed: GameSpeed = GameSpeed()) {


        fun cycleSpeed() {
            gameSpeed.cycleUp()
        }

        fun getVerticalCellCount(): Int {
            return minVerticalCellCount
        }

        fun getHorizontalCellCount(): Int {
            return minHorizontalCellCount
        }
    }
}