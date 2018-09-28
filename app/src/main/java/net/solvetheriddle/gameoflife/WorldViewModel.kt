package net.solvetheriddle.gameoflife

import java.util.*

/**
 * Model holding data about the current state of the [WorldView]
 */
class WorldViewModel(
        internal var gridVisible: Boolean = false,
        internal var zoom: Float = 1F) {


    private val stateObservable = StateObservable()
    fun registerStateObserver(observer: Observer) = stateObservable.addObserver(observer)
    class StateObservable : Observable() {
        internal var state: Array<IntArray> = arrayOf(IntArray(0))
            set(value) {
                field = value
                setChanged()
                notifyObservers()
            }
    }

    fun setState(state: Array<IntArray>) {
        stateObservable.state = state
    }
    fun getState(): Array<IntArray> {
        return stateObservable.state
    }


    fun getCellSize() = WorldConfig.MIN_CELL_SIZE

    fun getCellSizeScaled() = getCellSize() / zoom

    fun getWorldSizeRestriction(): Float {
        return (getCellSize() * WorldConfig.MIN_WORLD_SIZE).toFloat()
    }
}