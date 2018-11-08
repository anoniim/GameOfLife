package net.solvetheriddle.gameoflife.engine

import net.solvetheriddle.gameoflife.engine.WorldViewConfig.Companion.CELL_SIZE
import net.solvetheriddle.gameoflife.engine.WorldViewConfig.Companion.WORLD_SIZE_DEFAULT
import net.solvetheriddle.gameoflife.engine.WorldViewConfig.Companion.WRAP_EDGES_DEFAULT
import java.util.*

class Game {

    private val tag = this::class.java.simpleName

    enum class GameState {
        PAUSED, RUNNING
    }

    private lateinit var engine: GameEngine

    lateinit var worldStateObserver: ((state: WorldState) -> Unit)

    private fun notifyListeners(state: WorldState) {
        worldStateObserver(state)
    }

    fun new(settings: Settings) {
        engine = GameEngine(settings = settings) { _, _, state: WorldState -> notifyListeners(state) }
    }

    fun togglePlay() {
        engine.togglePlay()
    }

    fun getSettings(): Settings {
        return engine.settings
    }

    class Settings(
            private var rowCount: Int = WORLD_SIZE_DEFAULT,
            private var colCount: Int = WORLD_SIZE_DEFAULT,
            private val worldInitState: WorldInitState = WorldInitState.RANDOM,
            val wrapEdges: Boolean = WRAP_EDGES_DEFAULT) {


        val gameSpeed: GameSpeed = GameSpeed()

        enum class WorldInitState {
            RANDOM,
            EMPTY,
            CUSTOM
        }

        fun getInitState(): WorldState {
            return when (worldInitState) {
                WorldInitState.RANDOM -> WorldFactory.random(rowCount, colCount)
                WorldInitState.EMPTY -> WorldFactory.empty(rowCount, colCount)
                WorldInitState.CUSTOM -> WorldFactory.custom()
            }
        }

        fun cycleSpeed() {
            gameSpeed.cycleUp()
        }

        companion object {
            operator fun invoke(displaySize: DisplaySize,
                                worldInitState: WorldInitState = WorldInitState.RANDOM,
                                wrapEdges: Boolean = WRAP_EDGES_DEFAULT) = Settings(
                    displaySize.x / CELL_SIZE, displaySize.y / CELL_SIZE,
                    worldInitState, wrapEdges)
        }
    }
}

class WorldFactory {
    companion object {

        /**
         * Generates an empty world of given size ([rowCount] x [colCount]).
         */
        fun empty(rowCount: Int = 0, colCount: Int = 0): WorldState = Array(rowCount) { IntArray(colCount) }

        /**
         * Generates a random world of given size ([rowCount] x [colCount]).
         */
        fun random(rowCount: Int, colCount: Int): WorldState {
            return Array(rowCount) {
                IntArray(colCount) {
                    // Dead or alive?
                    Random().nextInt(2)
                }
            }
        }

        fun custom(): WorldState {
            TODO("not implemented")
        }
    }
}

class DisplaySize(val x: Int, val y: Int)
