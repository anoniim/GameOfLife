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
import kotlin.math.floor


class WorldView(context: Context, attrs: AttributeSet) : View(context, attrs) {

    private val model = WorldViewModel({ _, _, _ ->
        invalidate()
        requestLayout()
    })

    // The current destination rectangle (in pixel coordinates) into which the chart data should be drawn.
    private val content: RectF = RectF()
    // The current viewport. This rectangle represents the currently visible chart domain and range.
    private val viewport: RectF = RectF(content)

    private val gestureHelper = GestureHelper(this, content, viewport, model, context as GestureHelper.ClickListener)

    // Buffers used during drawing. These are defined as fields to avoid allocation during
    // draw calls.
    private var mAxisXPositionsBuffer = floatArrayOf()
    private var mAxisYPositionsBuffer = floatArrayOf()
    private var mAxisXLinesBuffer = floatArrayOf()
    private var mAxisYLinesBuffer = floatArrayOf()


    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        content.set(
                paddingLeft.toFloat(),
                paddingTop.toFloat(),
                (width - paddingLeft - paddingRight).toFloat(),
                (height - paddingTop - paddingBottom).toFloat())
        viewport.set(content)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (model.gridVisible) drawGrid(canvas)

        drawState(canvas, model.state)

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
            gestureHelper.animateZoom(zoom)
            invalidate()
            requestLayout()
        } else throw IllegalArgumentException("Invalid zoom")
    }

    fun setState(state: WorldState) {
        model.state = state
    }

    fun getState(): WorldState {
        return model.state
    }

    fun getDisplaySize(): Point {
        val size = Point()
        display.getRealSize(size)
        return size
    }

    fun showGrid(visible: Boolean) {
        model.gridVisible = visible
    }

    private fun Int.isAlive(): Boolean {
        return this > 0
    }

    private fun drawState(canvas: Canvas, state: WorldState) {
        val firstVisibleIndexY = (viewport.top / model.cellSize).toInt()
        val lastVisibleIndexY = (viewport.bottom / model.cellSize).toInt() - 1
        val firstVisibleIndexX = (viewport.left / model.cellSize).toInt()
        val lastVisibleIndexX = (viewport.right / model.cellSize).toInt() - 1
        for (y in firstVisibleIndexY..lastVisibleIndexY) {
            for (x in firstVisibleIndexX..lastVisibleIndexX) {
                if (state[x][y].isAlive()) {
                    drawCell(canvas, x, y)
                }
            }
        }
    }

    private fun drawCell(canvas: Canvas, x: Int, y: Int) {
        var cellLeft = x * model.cellSize - viewport.left
        if (cellLeft < content.left) cellLeft = content.left
        var cellTop = y * model.cellSize - viewport.top
        if (cellTop < content.top) cellTop = content.top
        var cellRight = (x * model.cellSize - viewport.left + model.cellSize) / model.zoom
        if (cellRight > content.right) cellRight = content.right
        var cellBottom = (y * model.cellSize - viewport.top + model.cellSize) / model.zoom
        if (cellBottom > content.bottom) cellBottom = content.bottom
        canvas.drawRect(RectF(
                cellLeft / model.zoom,
                cellTop / model.zoom,
                cellRight,
                cellBottom
        ), cellPaint)
    }

    private fun drawGrid(canvas: Canvas) {

        val newNumCellsX = Math.nextUp(viewport.width() / model.cellSize).toInt() + 1
        val newNumCellsY = Math.nextUp(viewport.height() / model.cellSize).toInt() + 1
        val mXStopsBuffer = FloatArray(newNumCellsX) { x -> x * model.cellSize.toFloat() }
        val mYStopsBuffer = FloatArray(newNumCellsY) { y -> y * model.cellSize.toFloat() }

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
        var i = 0
        val offsetX = model.cellSize - viewport.left.rem(model.cellSize)
        while (i < mXStopsBuffer.size) {
            mAxisXPositionsBuffer[i] = (mXStopsBuffer[i] + offsetX) / model.zoom
            i++
        }
        i = 0
        val offsetY = model.cellSize - viewport.top.rem(model.cellSize)
        while (i < mYStopsBuffer.size) {
            mAxisYPositionsBuffer[i] = (mYStopsBuffer[i] + offsetY) / model.zoom
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

    private fun draw(canvas: Canvas, rect: RectF) {
        canvas.drawRect(rect, helpPaint)
    }
}
