package net.solvetheriddle.gameoflife

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Point
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import java.util.*
import kotlin.math.floor


class WorldView(context: Context, attrs: AttributeSet) : View(context, attrs) {

    private var isGridVisible = true
    private var state = emptyArray<IntArray>()

    // The current destination rectangle (in pixel coordinates) into which the chart data should be drawn.
    val content: RectF = RectF()
    // The current viewport. This rectangle represents the currently visible chart domain and range.
    val viewport = RectF(content)

    private val gestureHelper = GestureHelper(this, content, viewport, context as GestureHelper.ClickListener)

    // Buffers used during drawing. These are defined as fields to avoid allocation during
    // draw calls.
    private var mAxisXPositionsBuffer = floatArrayOf()
    private var mAxisYPositionsBuffer = floatArrayOf()
    private var mAxisXLinesBuffer = floatArrayOf()
    private var mAxisYLinesBuffer = floatArrayOf()

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        content.right = (width - paddingLeft - paddingRight).toFloat()
        content.bottom = (height - paddingTop - paddingBottom).toFloat()

        content.set(
                paddingLeft.toFloat(),
                paddingTop.toFloat(),
                (width - paddingRight).toFloat(),
                (height - paddingBottom).toFloat())
        viewport.set(content)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (isGridVisible) drawGrid(canvas)
        drawState(canvas, state)

        // Draws axes and text labels
//        drawAxes(canvas)

        // Clips the next few drawing operations to the content area
        val clipRestoreCount = canvas.save()
        canvas.clipRect(content)

        gestureHelper.drawEdgeEffectsUnclipped(canvas)

        // Removes clipping rectangle
        canvas.restoreToCount(clipRestoreCount)

//        draw(canvas, viewport)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        return gestureHelper.onTouchEvent(event) || super.onTouchEvent(event)
    }


    override fun computeScroll() {
        super.computeScroll()
        gestureHelper.computeScroll()
    }

    fun setZoom(zoom: Float) {
        if (zoom in 0..1) {
            gestureHelper.setZoom(zoom)
            invalidate()
            requestLayout()
        } else throw IllegalArgumentException("Invalid zoom")
    }

    fun getZoom(): Float {
        return gestureHelper.getZoom()
    }

    fun setState(state: Array<IntArray>) {
        this.state = state
        invalidate()
        requestLayout()
    }

    fun getState(): Array<IntArray> {
        return state
    }

    fun getDisplaySize(): Point {
        val size = Point()
        display.getRealSize(size)
        return size
    }

    private fun getCurrCellCount(dimension: Int) = (dimension / WorldConfig.MIN_CELL_SIZE * getZoom()).toInt() + 1

    fun drawState(canvas: Canvas, state: Array<IntArray>) {
        val firstVisibleIndexY = (viewport.top / WorldConfig.MIN_CELL_SIZE).toInt()
        val lastVisibleIndexY = (viewport.bottom / WorldConfig.MIN_CELL_SIZE).toInt() - 1
        val firstVisibleIndexX = (viewport.left / WorldConfig.MIN_CELL_SIZE).toInt()
        val lastVisibleIndexX = (viewport.right / WorldConfig.MIN_CELL_SIZE).toInt() - 1
        for (y in firstVisibleIndexY..lastVisibleIndexY) {
            for (x in firstVisibleIndexX..lastVisibleIndexX) {
                if (state[x][y].isAlive()) {
                    drawCell(canvas, x, y)
                }
            }
        }
    }

    private fun drawCell(canvas: Canvas, x: Int, y: Int) {
        var cellLeft = x * WorldConfig.MIN_CELL_SIZE - viewport.left
        if (cellLeft < content.left) cellLeft = content.left
        var cellTop = y * WorldConfig.MIN_CELL_SIZE - viewport.top
        if (cellTop < content.top) cellTop = content.top
        var cellRight = (x * WorldConfig.MIN_CELL_SIZE - viewport.left + WorldConfig.MIN_CELL_SIZE) / getZoom()
        if (cellRight > content.right) cellRight = content.right
        var cellBottom = (y * WorldConfig.MIN_CELL_SIZE - viewport.top + WorldConfig.MIN_CELL_SIZE) / getZoom()
        if (cellBottom > content.bottom) cellBottom = content.bottom
        canvas.drawRect(RectF(
                cellLeft / getZoom(),
                cellTop / getZoom(),
                cellRight,
                cellBottom
        ), cellPaint)
    }

    fun drawGrid(canvas: Canvas) {

        // Computes axis stops (in terms of numerical value and position on screen)
        var i: Int

        val newNumCellsX = Math.nextUp(viewport.width() / WorldConfig.MIN_CELL_SIZE).toInt() + 1
        val newNumCellsY = Math.nextUp(viewport.height() / WorldConfig.MIN_CELL_SIZE).toInt() + 1
        val mXStopsBuffer = FloatArray(newNumCellsX) { x -> x * WorldConfig.MIN_CELL_SIZE.toFloat() }
        val mYStopsBuffer = FloatArray(newNumCellsY) { y -> y * WorldConfig.MIN_CELL_SIZE.toFloat() }

        // Avoid unnecessary allocations during drawing. Re-use allocated
        // arrays and only reallocate if the number of stops grows.
        if (mAxisXPositionsBuffer.size < mXStopsBuffer.size) {
            mAxisXPositionsBuffer = FloatArray(mXStopsBuffer.size)
        }
        if (mAxisYPositionsBuffer.size < mYStopsBuffer.size) {
            mAxisYPositionsBuffer = FloatArray(mYStopsBuffer.size)
        }
        if (mAxisXLinesBuffer.size < mXStopsBuffer.size * 4) {
            mAxisXLinesBuffer = FloatArray(mXStopsBuffer.size * 4)
        }
        if (mAxisYLinesBuffer.size < mYStopsBuffer.size * 4) {
            mAxisYLinesBuffer = FloatArray(mYStopsBuffer.size * 4)
        }

        // Compute positions
        i = 0
        val offsetX = WorldConfig.MIN_CELL_SIZE - viewport.left.rem(WorldConfig.MIN_CELL_SIZE)
        while (i < mXStopsBuffer.size) {
            mAxisXPositionsBuffer[i] = (mXStopsBuffer[i] + offsetX) / getZoom()
            i++
        }
        i = 0
        val offsetY = WorldConfig.MIN_CELL_SIZE - viewport.top.rem(WorldConfig.MIN_CELL_SIZE)
        while (i < mYStopsBuffer.size) {
            mAxisYPositionsBuffer[i] = (mYStopsBuffer[i] + offsetY) / getZoom()
            i++
        }

        // Draws grid lines using drawLines (faster than individual drawLine calls)
        i = 0
        while (i < mXStopsBuffer.size) {
            mAxisXLinesBuffer[i * 4 + 0] = floor(mAxisXPositionsBuffer[i])
            mAxisXLinesBuffer[i * 4 + 1] = content.top
            mAxisXLinesBuffer[i * 4 + 2] = floor(mAxisXPositionsBuffer[i])
            mAxisXLinesBuffer[i * 4 + 3] = content.bottom
            i++
        }
        canvas.drawLines(mAxisXLinesBuffer, 0, mXStopsBuffer.size * 4, gridPaint)

        i = 0
        while (i < mYStopsBuffer.size) {
            mAxisYLinesBuffer[i * 4 + 0] = content.left
            mAxisYLinesBuffer[i * 4 + 1] = floor(mAxisYPositionsBuffer[i])
            mAxisYLinesBuffer[i * 4 + 2] = content.right
            mAxisYLinesBuffer[i * 4 + 3] = floor(mAxisYPositionsBuffer[i])
            i++
        }
        canvas.drawLines(mAxisYLinesBuffer, 0, mYStopsBuffer.size * 4, gridPaint)
    }

    private val helpPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.help)
    }

    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.grid_line)
    }

    private val cellPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.cell_color)
    }

    private fun Int.isVisible(): Boolean {
        return this > 0
    }

    private fun Int.isAlive(): Boolean {
        return this > 0
    }

    fun draw(canvas: Canvas, rect: RectF) {
        canvas.drawRect(rect, helpPaint)
    }

    fun showGrid(visible: Boolean) {
        isGridVisible = visible
    }

    /**
     * A simple class representing axis label values.
     *
     * @see .computeAxisStops
     */
    private data class Cell(
            internal var stops: FloatArray = floatArrayOf(),
            internal var numStops: Int = 0,
            internal var decimals: Int = 0) {

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Cell

            if (!Arrays.equals(stops, other.stops)) return false
            if (numStops != other.numStops) return false
            if (decimals != other.decimals) return false

            return true
        }

        override fun hashCode(): Int {
            var result = Arrays.hashCode(stops)
            result = 31 * result + numStops
            result = 31 * result + decimals
            return result
        }
    }
}
