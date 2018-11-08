package net.solvetheriddle.gameoflife.engine

import java.util.*

data class GameSpeed(
        var millis: Int = MAX_STEP_MILLIS)
    : Observable() {

    fun cycleUp() {
        if (millis >= MIN_STEP_MILLIS) {
            millis /= STEP_MILLIS
        } else {
            millis = MAX_STEP_MILLIS
        }
        setChanged()
        notifyObservers(getSpeedCoefficient(millis))
    }

    private fun getSpeedCoefficient(currentMillis: Int) = numberOfSteps - log2nlz(currentMillis) + 2

    private val numberOfSteps: Int = log2nlz(MAX_STEP_MILLIS) - 1

    private fun log2nlz(bits: Int): Int {
        return if (bits == 0) 0 else 31 - Integer.numberOfLeadingZeros(bits)
    }

    companion object {
        private const val STEP_MILLIS: Int = 2
        private const val MIN_STEP_MILLIS: Int = 8
        private const val MAX_STEP_MILLIS: Int = 512
    }
}
