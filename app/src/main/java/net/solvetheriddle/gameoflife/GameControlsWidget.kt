package net.solvetheriddle.gameoflife

import android.animation.Animator
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import kotlinx.android.synthetic.main.activity_game.view.*


class GameControlsWidget
        @JvmOverloads
        constructor(context: Context,
                    attrs: AttributeSet? = null,
                    defStyleAttr: Int = 0) : ConstraintLayout(context, attrs, defStyleAttr) {

    private var playButtonVisible: Boolean = true
    private var controlsVisible: Boolean = false

    internal fun toggleControlsVisibility() {
        if (playButtonVisible) hide() else show()
    }

    internal fun toggleExtraControlsVisibility(): Boolean {
        extra_controls_view.animateVisibility(controlsVisible)
        controlsVisible = !controlsVisible
        return true
    }

    private fun View.animateVisibility(currentlyVisible: Boolean) {
        ObjectAnimator.ofFloat(this, "alpha", if (currentlyVisible) 0F else 1F)
                .apply {
                    duration = UI_ANIMATION_SPEED
                    start()
                }
                .addListener(object: Animator.AnimatorListener {
                    override fun onAnimationRepeat(p0: Animator?) {}
                    override fun onAnimationCancel(p0: Animator?) {}
                    override fun onAnimationStart(p0: Animator?) {}
                    override fun onAnimationEnd(p0: Animator?) {
                        visibility = if(currentlyVisible) View.GONE else View.VISIBLE
                    }
                })
    }

    @SuppressLint("RestrictedApi")
    fun hide() {
        play_button.visibility = View.GONE
        playButtonVisible = false
        controls_view.visibility = View.GONE
        controlsVisible = false
    }

    @SuppressLint("RestrictedApi")
    fun show() {
        play_button.visibility = View.VISIBLE
        playButtonVisible = true
        controls_view.visibility = View.VISIBLE
        controlsVisible = true
    }

    companion object {
        private const val UI_ANIMATION_SPEED = 300L
    }
}