package net.solvetheriddle.gameoflife

import android.os.SystemClock
import android.util.Log
import androidx.annotation.WorkerThread
import net.solvetheriddle.gameoflife.view.WorldViewConfig
import kotlin.properties.Delegates

class GameEngine(
        private val nextTurnStrategy: NextTurnStrategy = NextTurnStrategy.classic(),
        val settings: Game.Settings,
        worldStateObserver: WorldStateObserver
) {

    private val tag = this::class.java.simpleName

    private var worldState: WorldState
            by Delegates.observable(settings.getInitState(), worldStateObserver)

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

        worldState = nextTurnStrategy.calculate(worldState)
    }

    private fun assertWorldValid(worldState: WorldState) {
        if (worldState.size < WorldViewConfig.WORLD_SIZE_MIN || worldState[0].size < WorldViewConfig.WORLD_SIZE_MIN)
            throw IllegalStateException("Size must be at least ${WorldViewConfig.WORLD_SIZE_MIN}, is "
                    + if (worldState.isEmpty()) "0" else "[${worldState.size}, ${worldState[0].size}]")
    }
}

typealias Liveness = (cellValue: Int) -> Boolean
typealias Neighborhood = (state: WorldState, i: Int, j: Int, isAlive: Liveness) -> Int
typealias Rules = (state: WorldState, i: Int, j: Int, numOfNeighbors: Int, isAlive: Liveness) -> Int

class NextTurnStrategy(internal val liveness: Liveness,
                       internal val neighborhood: Neighborhood,
                       internal val rules: Rules) {

    private lateinit var _nextState: WorldState

    fun calculate(worldState: WorldState): WorldState {
        _nextState = WorldFactory.empty(worldState.size, worldState[0].size)
        for (i in worldState.indices) {
            for (j in 0 until worldState[0].size) {
                val numOfNeighbours = neighborhood(worldState, i, j, liveness)
                _nextState[i][j] = rules(worldState, i, j, numOfNeighbours, liveness)
            }
        }
        return _nextState.copyOf()
    }

    companion object {

        fun classic(): NextTurnStrategy {
            return NextTurnStrategy(
                    liveness = ::positiveIsAlive,
                    neighborhood = ::infiniteEdges,
                    rules = ::conwaysRules
            )
        }

        private fun positiveIsAlive(cellValue: Int): Boolean {
            return cellValue > 0
        }

        private fun neighborsWithLoops(state: WorldState, i: Int, j: Int, isAlive: Liveness): Int {
            val xBoundary = state.size - 1
            val yBoundary = state[0].size - 1
            var numOfNeighbours = 0
            for (x in -1..1) {
                val xNeighbor = i + x
                if (xNeighbor in 0..xBoundary) {
                    for (y in -1..1) {
                        val yNeighbor = j + y
                        if (yNeighbor in 0..yBoundary) {
                            if (x == 0 && y == 0) continue // Do not count itself
                            if (isAlive(state[xNeighbor][yNeighbor])) numOfNeighbours++
                        }
                    }
                }
            }
            return numOfNeighbours
        }

        private fun infiniteEdges(state: WorldState, i: Int, j: Int, isAlive: Liveness): Int {
            val xBoundary = state.size - 1
            val yBoundary = state[0].size - 1
            var numOfNeighbours = 0
            for (x in -1..1) {
                val xNeighbor = (i + x + state.size) % state.size
                if (xNeighbor in 0..xBoundary) {
                    for (y in -1..1) {
                        val yNeighbor = (j + y + state.size) % state.size
                        if (yNeighbor in 0..yBoundary) {
                            if (x == 0 && y == 0) continue // Do not count itself
                            if (isAlive(state[xNeighbor][yNeighbor])) numOfNeighbours++
                        }
                    }
                }
            }
            return numOfNeighbours
        }

        private fun conwaysRules(state: WorldState, i: Int, j: Int, numOfNeighbors: Int, isAlive: Liveness): Int {
            return if (isAlive(state[i][j])) {
                if (numOfNeighbors in 2..3) 1 else 0
            } else {
                if (numOfNeighbors == 3) 1 else 0
            }
        }

        private fun neighborsNoLoop(state: WorldState, i: Int, j: Int, liveness: Liveness): Int {
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
            if (!isUpperEdge && !isLeftEdge && liveness(state[upperNeighbor][leftNeighbor])) {
                numOfNeighbours++
            }
            if (!isUpperEdge && liveness(state[upperNeighbor][j])) {
                numOfNeighbours++
            }
            if (!isUpperEdge && !isRightEdge && liveness(state[upperNeighbor][rightNeighbor])) {
                numOfNeighbours++
            }
            if (!isLeftEdge && liveness(state[i][leftNeighbor])) {
                numOfNeighbours++
            }
            if (!isRightEdge && liveness(state[i][rightNeighbor])) {
                numOfNeighbours++
            }
            if (!isLowerEdge && !isLeftEdge && liveness(state[lowerNeighbor][leftNeighbor])) {
                numOfNeighbours++
            }
            if (!isLowerEdge && liveness(state[lowerNeighbor][j])) {
                numOfNeighbours++
            }
            if (!isLowerEdge && !isRightEdge && liveness(state[lowerNeighbor][rightNeighbor])) {
                numOfNeighbours++
            }

            return numOfNeighbours
        }
    }
}