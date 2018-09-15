package net.solvetheriddle.gameoflife

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Paint.ANTI_ALIAS_FLAG
import android.graphics.Point
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat


class WorldView(context: Context, attrs: AttributeSet) : View(context, attrs) {

    companion object Config {
        private const val LINE_WIDTH = 2
        private const val CELL_SIZE = 10
    }

    private var zoom = 1
    private var state = emptyArray<IntArray>()
    private var verticalLines: Array<Rect> = emptyArray()
    private var horizontalLines: Array<Rect> = emptyArray()

    private var isGridVisible = true


    private val gridPaint = Paint(ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.grid_line)
    }

    private val cellPaint = Paint(ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.cell_color)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        initializeVerticalLines(width, height)
        initializeHorizontalLines(width, height)
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        canvas?.apply {
            if(isGridVisible) drawGrid()
            drawState()
        }
    }

    private fun initializeVerticalLines(width: Int, height: Int) {
        val numOfLines = getCellCount(width)
        verticalLines = Array(numOfLines) {
            Rect(
                    it * CELL_SIZE,
                    0,
                    it * CELL_SIZE + LINE_WIDTH,
                    height)
        }
    }

    private fun initializeHorizontalLines(width: Int, height: Int) {
        val numOfLines = getCellCount(height)
        horizontalLines = Array(numOfLines) {
            Rect(
                    0,
                    it * CELL_SIZE,
                    width,
                    it * CELL_SIZE + LINE_WIDTH)
        }
    }

    private fun Canvas.drawState() {
        for (rowIndex in state.indices) {
            for (columnIndex in state[0].indices) {
                if (state[rowIndex][columnIndex].isAlive()) {
                    drawCell(rowIndex, columnIndex)
                }
            }
        }
    }

    private fun Canvas.drawCell(rowIndex: Int, columnIndex: Int) {
        drawRect(Rect(
                columnIndex * CELL_SIZE,
                rowIndex * CELL_SIZE,
                columnIndex * CELL_SIZE + CELL_SIZE,
                rowIndex * CELL_SIZE + CELL_SIZE
        ), cellPaint)
    }

    private fun Canvas.drawGrid() {
        for (line in verticalLines) {
            drawRect(line, gridPaint)
        }
        for (line in horizontalLines) {
            drawRect(line, gridPaint)
        }
    }

    fun setZoom(zoom: Int) {
        this.zoom = zoom
        invalidate()
        requestLayout()
    }

    fun getZoom(): Int {
        return zoom
    }

    fun setState(state: Array<IntArray>) {
        this.state = state
        invalidate()
        requestLayout()
    }

    fun getState(): Array<IntArray> {
        return state
    }

    fun getCellCount(dimension: Int) = dimension / CELL_SIZE

    fun setGridVisibility(gridVisibility: Boolean) {
        isGridVisible = gridVisibility
    }

    fun getDisplaySize(): Point {
        val size = Point()
        display.getRealSize(size)
        return size
    }
}

private fun Int.isAlive(): Boolean {
    return this > 0
}
