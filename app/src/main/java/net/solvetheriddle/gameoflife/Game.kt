package net.solvetheriddle.gameoflife

import net.solvetheriddle.gameoflife.view.WorldConfig.Companion.WORLD_SIZE_DEFAULT
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
        engine = GameEngine(settings) { _, _, state: WorldState -> notifyListeners(state) }
    }

    fun togglePlay() {
        engine.togglePlay()
    }

    fun getSettings(): Settings {
        return engine.settings
    }

    class Settings(
            private var colCount: Int = WORLD_SIZE_DEFAULT,
            private var rowCount: Int = WORLD_SIZE_DEFAULT,
            private val worldInitState: WorldInitState = WorldInitState.RANDOM,
            var wrapEdges: Boolean = false) {

        val gameSpeed: GameSpeed = GameSpeed()

        enum class WorldInitState {
            RANDOM,
            EMPTY,
            CUSTOM
        }

        fun getInitState(): WorldState {
            return when(worldInitState) {
                WorldInitState.RANDOM -> WorldFactory.random(colCount, rowCount)
                WorldInitState.EMPTY -> WorldFactory.empty(colCount, rowCount)
                WorldInitState.CUSTOM -> TODO()
            }
        }

        fun cycleSpeed() {
            gameSpeed.cycleUp()
        }
    }
}

class WorldFactory {
    companion object {

        fun empty(colCount: Int = 0, rowCount: Int = 0): WorldState = Array(colCount) { IntArray(rowCount) }

        fun random(colCount: Int, rowCount: Int): WorldState {
            return Array(colCount) { _ ->
                IntArray(rowCount) {
                    Random().nextInt(2)
                }
            }
        }
    }
}