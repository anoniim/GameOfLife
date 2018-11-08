package net.solvetheriddle.gameoflife.view

import net.solvetheriddle.gameoflife.engine.WorldFactory
import net.solvetheriddle.gameoflife.engine.WorldState
import net.solvetheriddle.gameoflife.engine.WorldStateObserver
import kotlin.properties.Delegates

/**
 * Model holding data about the current state of the [WorldView]
 */
class WorldViewModel(
        stateObserver: net.solvetheriddle.gameoflife.engine.WorldStateObserver,
        internal var gridVisible: Boolean = net.solvetheriddle.gameoflife.engine.WorldViewConfig.GRID_VISIBLE_DEFAULT,
        internal var zoom: Float = net.solvetheriddle.gameoflife.engine.WorldViewConfig.ZOOM_DEFAULT) {

    var state: net.solvetheriddle.gameoflife.engine.WorldState by Delegates.observable(net.solvetheriddle.gameoflife.engine.WorldFactory.empty(), stateObserver)

    val cellSize = net.solvetheriddle.gameoflife.engine.WorldViewConfig.CELL_SIZE

    val cellSizeScaled = cellSize / zoom

    val worldSizeRestriction = (cellSize * net.solvetheriddle.gameoflife.engine.WorldViewConfig.WORLD_SIZE_MIN).toFloat()
}