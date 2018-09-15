package net.solvetheriddle.gameoflife

import android.os.Bundle
import android.os.Handler
import android.view.View
import android.view.ViewTreeObserver
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import kotlinx.android.synthetic.main.activity_game.*
import kotlinx.android.synthetic.main.activity_game.view.*
import kotlin.concurrent.thread

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
class GameActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_game)

        val viewModel = ViewModelProviders.of(this).get(GameViewModel::class.java)

        initWorldView(viewModel)
        initGameControls(viewModel)

        // World state observer
        viewModel.gameLiveData.observe(this, Observer {
            runOnUiThread {
                world_view.setState(it)
            }
        })
        viewModel.gameLiveData.gameSpeed.observe(this, Observer {
            runOnUiThread {
                Toast.makeText(this, "$it x", Toast.LENGTH_SHORT).show()
            }
        })
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        delayedHide()
    }

    private fun initWorldView(viewModel: GameViewModel) {
        world_view.setOnClickListener {
            toggleSystemUiVisibility()
        }
        world_view.setGridVisibility(false)
        world_view.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                val displaySize = world_view.getDisplaySize()
                world_view.setState(viewModel.initGame(
                        world_view.getCellCount(displaySize.y),
                        world_view.getCellCount(displaySize.x)))
                world_view.viewTreeObserver.removeOnGlobalLayoutListener(this)
            }
        })
    }

    private fun initGameControls(viewModel: GameViewModel) {
        controls_view.play_button.setOnClickListener {
            thread { viewModel.togglePlay() }
        }
        controls_view.play_button.setOnLongClickListener {
            controls_view.toggleExtraControlsVisibility()
        }
        controls_view.zoom_button.setOnClickListener {
            viewModel.cycleZoom()
        }
        controls_view.speed_button.setOnClickListener {
            viewModel.cycleSpeed()
        }
    }

    private var systemUiShown: Boolean = true
    private val mHideHandler = Handler()
    private val mDelayedHideRunnable = Runnable { hideUi() }
    private val mHidePart2Runnable = Runnable { hideSystemUi() }
    private val mShowPart2Runnable = Runnable { controls_view.show() }

    private fun toggleSystemUiVisibility() {
        if (systemUiShown) hideUi() else showUi()
    }

    private fun delayedHide() {
        mHideHandler.removeCallbacks(mDelayedHideRunnable)
        mHideHandler.postDelayed(mDelayedHideRunnable, INITIAL_HIDE_DELAY)
    }

    private fun hideUi() {
        controls_view.hide()
        // Schedule a runnable to remove the status and navigation bar after a delay
        mHideHandler.removeCallbacks(mShowPart2Runnable)
        mHideHandler.postDelayed({ hideSystemUi() }, UI_ANIMATION_DELAY)
        systemUiShown = false
    }

    private fun hideSystemUi() {
        // Delayed removal of status and navigation bar
        window.decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_LOW_PROFILE or
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
    }

    private fun showUi() {
        showSystemUi()
        // Schedule a runnable to display UI elements after a delay
        mHideHandler.removeCallbacks(mHidePart2Runnable)
        mHideHandler.postDelayed(mShowPart2Runnable, UI_ANIMATION_DELAY)
        systemUiShown = true

    }

    private fun showSystemUi() {
        // Show the system bar
        window.decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
    }

    companion object {
        /**
         * Whether or not the system UI should be auto-hidden after
         * [AUTO_HIDE_DELAY_MILLIS] milliseconds.
         */
        private const val AUTO_HIDE = true

        /**
         * If [AUTO_HIDE] is set, the number of milliseconds to wait after
         * user interaction before hiding the system UI.
         */
        private const val AUTO_HIDE_DELAY_MILLIS = 3000

        /**
         * Some older devices needs a small delay between UI widget updates
         * and a change of the status and navigation bar.
         */
        private const val UI_ANIMATION_DELAY = 300L

        private const val INITIAL_HIDE_DELAY = 100L
    }
}
