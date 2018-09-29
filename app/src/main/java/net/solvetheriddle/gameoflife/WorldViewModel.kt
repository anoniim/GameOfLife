package net.solvetheriddle.gameoflife

import kotlin.properties.Delegates
import kotlin.reflect.KProperty

typealias StateObserver = (property: KProperty<*>, oldValue: WorldState, newValue: WorldState) -> Unit

/**
 * Model holding data about the current state of the [WorldView]
 */
class WorldViewModel(
        stateObserver: StateObserver,
        internal var gridVisible: Boolean = WorldConfig.GRID_VISIBLE_DEFAULT,
        internal var zoom: Float = WorldConfig.ZOOM_DEFAULT) {

    var state: WorldState by Delegates.observable(WorldFactory.empty(), stateObserver)

    val cellSize = WorldConfig.CELL_SIZE

    val cellSizeScaled = cellSize / zoom

    val worldSizeRestriction = (cellSize * WorldConfig.WORLD_SIZE_MIN).toFloat()
}