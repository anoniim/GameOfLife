package net.solvetheriddle.gameoflife

import androidx.lifecycle.LiveData

data class GameSpeed(var millis: Int = MAX_STEP_MILLIS): LiveData<Int>() {

    companion object {
        private const val STEP_MILLIS: Int = 2
        private const val MAX_STEP_MILLIS: Int = 512
        private const val MIN_STEP_MILIS: Int = 8
    }

    fun cycleUp() {
        if (millis >= MIN_STEP_MILIS) {
            millis /= STEP_MILLIS
        } else {
            millis = MAX_STEP_MILLIS
        }
        value = speedMultiplicator(millis)
    }

    private fun speedMultiplicator(currentMillis: Int) = numberOfSteps - log2nlz(currentMillis) + 2

    private val numberOfSteps: Int = log2nlz(MAX_STEP_MILLIS) - 1

    private fun log2nlz(bits: Int): Int {
        return if (bits == 0) 0 else 31 - Integer.numberOfLeadingZeros(bits)
    }
}
