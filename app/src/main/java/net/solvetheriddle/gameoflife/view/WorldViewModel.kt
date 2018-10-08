package net.solvetheriddle.gameoflife.view

import net.solvetheriddle.gameoflife.WorldFactory
import net.solvetheriddle.gameoflife.WorldState
import net.solvetheriddle.gameoflife.WorldStateObserver
import kotlin.properties.Delegates

/**
 * Model holding data about the current state of the [WorldView]
 */
class WorldViewModel(
        stateObserver: WorldStateObserver,
        internal var gridVisible: Boolean = WorldConfig.GRID_VISIBLE_DEFAULT,
        internal var zoom: Float = WorldConfig.ZOOM_DEFAULT) {

    var state: WorldState by Delegates.observable(WorldFactory.empty(), stateObserver)

    val cellSize = WorldConfig.CELL_SIZE

    val cellSizeScaled = cellSize / zoom

    val worldSizeRestriction = (cellSize * WorldConfig.WORLD_SIZE_MIN).toFloat()
}