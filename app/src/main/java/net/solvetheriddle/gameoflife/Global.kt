package net.solvetheriddle.gameoflife

import kotlin.reflect.KProperty

typealias WorldState = Array<IntArray>

typealias WorldStateObserver = (property: KProperty<*>, oldValue: WorldState, newValue: WorldState) -> Unit

private fun WorldState.equalSize(newState: WorldState): Boolean {
    return this.size == newState.size && this[0].size == newState[0].size
}

private fun WorldState.isEmpty(): Boolean {
    return this.isEmpty() || this[0].isEmpty()
}

