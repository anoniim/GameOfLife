package net.solvetheriddle.gameoflife.view

import android.graphics.Canvas
import android.graphics.Point
import android.graphics.PointF
import android.graphics.RectF
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.widget.EdgeEffect
import android.widget.OverScroller
import androidx.core.view.GestureDetectorCompat
import androidx.core.view.ViewCompat
import java.lang.Math.nextUp
import kotlin.math.max
import kotlin.math.min

class GestureHelper(
        private val view: View,
        private val content: RectF,
        private val viewport: RectF,
        private val model: WorldViewModel,
        private val clickListener: ClickListener?) {

    interface ClickListener {
        fun onSingleTap(e: MotionEvent)
        fun onDoubleTap(e: MotionEvent)
    }

    private val mScroller = OverScroller(view.context)
    private val mZoomer: Zoomer = Zoomer(view.context)
    private val mZoomFocalPoint = PointF()
    private val mScrollerStartViewport = RectF() // Used only for zooms and flings.

    private val mSurfaceSizeBuffer = Point()

    // Edge effect / overscroll tracking objects.
    private val mEdgeEffectTop: EdgeEffect = EdgeEffect(view.context)
    private val mEdgeEffectBottom: EdgeEffect = EdgeEffect(view.context)
    private val mEdgeEffectLeft: EdgeEffect = EdgeEffect(view.context)
    private val mEdgeEffectRight: EdgeEffect = EdgeEffect(view.context)

    private var mEdgeEffectTopActive: Boolean = false
    private var mEdgeEffectBottomActive: Boolean = false
    private var mEdgeEffectLeftActive: Boolean = false
    private var mEdgeEffectRightActive: Boolean = false


    fun onTouchEvent(event: MotionEvent?): Boolean {
        var retVal = scaleGestureDetector.onTouchEvent(event)
        retVal = gestureDetector.onTouchEvent(event) || retVal
        return retVal
    }

    /**
     * Draws the overscroll "glow" at the four edges of the chart region, if necessary. The edges
     * of the chart region are stored in [.content].
     *
     * @see EdgeEffect
     */
    fun drawEdgeEffectsUnclipped(canvas: Canvas) {
        // The methods below rotate and translate the canvas as needed before drawing the glow,
        // since EdgeEffectCompat always draws a top-glow at 0,0.

        var needsInvalidate = false

        if (!mEdgeEffectTop.isFinished) {
            val restoreCount = canvas.save()
            canvas.translate(content.left, content.top)
            mEdgeEffectTop.setSize(content.width().toInt(), content.height().toInt())
            if (mEdgeEffectTop.draw(canvas)) {
                needsInvalidate = true
            }
            canvas.restoreToCount(restoreCount)
        }

        if (!mEdgeEffectBottom.isFinished) {
            val restoreCount = canvas.save()
            canvas.translate((2 * content.left - content.right), content.bottom)
            canvas.rotate(180f, content.width(), 0f)
            mEdgeEffectBottom.setSize(content.width().toInt(), content.height().toInt())
            if (mEdgeEffectBottom.draw(canvas)) {
                needsInvalidate = true
            }
            canvas.restoreToCount(restoreCount)
        }

        if (!mEdgeEffectLeft.isFinished) {
            val restoreCount = canvas.save()
            canvas.translate(content.left, content.bottom)
            canvas.rotate(-90f, 0f, 0f)
            mEdgeEffectLeft.setSize(content.height().toInt(), content.width().toInt())
            if (mEdgeEffectLeft.draw(canvas)) {
                needsInvalidate = true
            }
            canvas.restoreToCount(restoreCount)
        }

        if (!mEdgeEffectRight.isFinished) {
            val restoreCount = canvas.save()
            canvas.translate(content.right, content.top)
            canvas.rotate(90f, 0f, 0f)
            mEdgeEffectRight.setSize(content.height().toInt(), content.width().toInt())
            if (mEdgeEffectRight.draw(canvas)) {
                needsInvalidate = true
            }
            canvas.restoreToCount(restoreCount)
        }

        if (needsInvalidate) {
            ViewCompat.postInvalidateOnAnimation(view)
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    //
    //     Methods and objects related to gesture handling
    //
    ////////////////////////////////////////////////////////////////////////////////////////////////

    private var gestureDetector = GestureDetectorCompat(view.context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onDown(e: MotionEvent): Boolean {
            releaseEdgeEffects()
            mScrollerStartViewport.set(viewport)
            mScroller.forceFinished(true)
            ViewCompat.postInvalidateOnAnimation(view)
            return true
        }

        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
            return if (clickListener != null) {
                clickListener.onSingleTap(e)
                true
            } else {
                false
            }
        }

        override fun onDoubleTap(e: MotionEvent): Boolean {
            if (clickListener != null) {
                clickListener.onDoubleTap(e)
            } else {
                mZoomer.forceFinished(true)
                if (hitTest(e.x, e.y, mZoomFocalPoint)) {
                    animateZoom(WorldConfig.AUTO_ZOOM_AMOUNT)
                }
                ViewCompat.postInvalidateOnAnimation(view)
            }
            return true
        }

        override fun onScroll(e1: MotionEvent, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
            // Scrolling uses math based on the viewport (as opposed to math using pixels).
            /**
             * Pixel offset is the offset in screen pixels, while viewport offset is the
             * offset within the current viewport. For additional information on surface sizes
             * and pixel offsets, see the docs for []. For
             * additional information about the viewport, see the comments for
             * [viewport].
             */
            val viewportOffsetX = distanceX * viewport.width() / content.width()
            val viewportOffsetY = distanceY * viewport.height() / content.height()
            computeScrollSurfaceSize(mSurfaceSizeBuffer)
            val scrolledX = (mSurfaceSizeBuffer.x * (viewport.left + viewportOffsetX - content.left) / (content.right - content.left)).toInt()
            val scrolledY = (mSurfaceSizeBuffer.y * (content.bottom - viewport.bottom - viewportOffsetY) / (content.bottom - content.top)).toInt()
            val canScrollX = viewport.left > content.left || viewport.right < content.right
            val canScrollY = viewport.top > content.top || viewport.bottom < content.bottom
            setViewportBottomLeft(
                    viewport.left + viewportOffsetX,
                    viewport.bottom + viewportOffsetY)
            ViewCompat.postInvalidateOnAnimation(view)

            if (canScrollX && scrolledX < 0) {
                mEdgeEffectLeft.onPull(scrolledX / content.width())
                mEdgeEffectLeftActive = true
            }
            if (canScrollY && scrolledY < 0) {
                mEdgeEffectTop.onPull(scrolledY / content.height())
                mEdgeEffectTopActive = true
            }
            if (canScrollX && scrolledX > mSurfaceSizeBuffer.x - content.width()) {
                mEdgeEffectRight.onPull((scrolledX - mSurfaceSizeBuffer.x + content.width()) / content.width())
                mEdgeEffectRightActive = true
            }
            if (canScrollY && scrolledY > mSurfaceSizeBuffer.y - content.height()) {
                mEdgeEffectBottom.onPull((scrolledY - mSurfaceSizeBuffer.y + content.height()) / content.height())
                mEdgeEffectBottomActive = true
            }
            return true
        }

        override fun onFling(e1: MotionEvent, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
            // FIXME both dimensions originally reversed (not only X)
            fling((-velocityX).toInt(), (velocityY).toInt())
            return true
        }
    })

    fun animateZoom(endZoom: Float) {
        mZoomer.startZoom(endZoom)
    }

    fun computeScroll() {
        var needsInvalidate = false

        if (mScroller.computeScrollOffset()) {
            // The scroller isn't finished, meaning a fling or programmatic pan operation is
            // currently active.

            computeScrollSurfaceSize(mSurfaceSizeBuffer)
            val currX = mScroller.currX
            val currY = mScroller.currY

            val canScrollX = viewport.left > content.left || viewport.right < content.right
            val canScrollY = viewport.top > content.top || viewport.bottom < content.bottom

            if (canScrollX
                    && currX < 0
                    && mEdgeEffectLeft.isFinished
                    && !mEdgeEffectLeftActive) {
                mEdgeEffectLeft.onAbsorb(mScroller.currVelocity.toInt())
                mEdgeEffectLeftActive = true
                needsInvalidate = true
            } else if (canScrollX
                    && currX > mSurfaceSizeBuffer.x - content.width()
                    && mEdgeEffectRight.isFinished
                    && !mEdgeEffectRightActive) {
                mEdgeEffectRight.onAbsorb(mScroller.currVelocity.toInt())
                mEdgeEffectRightActive = true
                needsInvalidate = true
            }

            if (canScrollY
                    && currY < 0
                    && mEdgeEffectTop.isFinished
                    && !mEdgeEffectTopActive) {
                mEdgeEffectTop.onAbsorb(mScroller.currVelocity.toInt())
                mEdgeEffectTopActive = true
                needsInvalidate = true
            } else if (canScrollY
                    && currY > mSurfaceSizeBuffer.y - content.height()
                    && mEdgeEffectBottom.isFinished
                    && !mEdgeEffectBottomActive) {
                mEdgeEffectBottom.onAbsorb(mScroller.currVelocity.toInt())
                mEdgeEffectBottomActive = true
                needsInvalidate = true
            }

            val currXRange = (content.left + content.width() * currX / mSurfaceSizeBuffer.x)
            val currYRange = (content.bottom - content.height() * currY / mSurfaceSizeBuffer.y)
            setViewportBottomLeft(currXRange, currYRange)
        }

        if (mZoomer.computeZoom()) {
            // Performs the zoom since a zoom is in progress (either programmatically or via
            // double-touch).
            val newWidth = (1f - mZoomer.currZoom) * mScrollerStartViewport.width()
            val newHeight = (1f - mZoomer.currZoom) * mScrollerStartViewport.height()
            val pointWithinViewportX = (mZoomFocalPoint.x - mScrollerStartViewport.left) / mScrollerStartViewport.width()
            val pointWithinViewportY = (mZoomFocalPoint.y - mScrollerStartViewport.top) / mScrollerStartViewport.height()
            viewport.set(
                    mZoomFocalPoint.x - newWidth * pointWithinViewportX,
                    mZoomFocalPoint.y - newHeight * pointWithinViewportY,
                    mZoomFocalPoint.x + newWidth * (1 - pointWithinViewportX),
                    mZoomFocalPoint.y + newHeight * (1 - pointWithinViewportY))
            constrainViewport()
            needsInvalidate = true
        }

        if (needsInvalidate) {
            ViewCompat.postInvalidateOnAnimation(view)
        }
    }


    private fun releaseEdgeEffects() {
        mEdgeEffectBottomActive = false
        mEdgeEffectRightActive = mEdgeEffectBottomActive
        mEdgeEffectTopActive = mEdgeEffectRightActive
        mEdgeEffectLeftActive = mEdgeEffectTopActive
        mEdgeEffectLeft.onRelease()
        mEdgeEffectTop.onRelease()
        mEdgeEffectRight.onRelease()
        mEdgeEffectBottom.onRelease()
    }

    /**
     * Computes the current scrollable surface size, in pixels. For example, if the entire chart
     * area is visible, this is simply the current size of [.content]. If the chart
     * is zoomed in 200% in both directions, the returned size will be twice as large horizontally
     * and vertically.
     */
    private fun computeScrollSurfaceSize(out: Point) {
        out.set(
                (content.width() * (content.right - content.left) / viewport.width()).toInt(),
                (content.height() * (content.bottom - content.top) / viewport.height()).toInt())
    }

    /**
     * Finds the chart point (i.e. within the chart's domain and range) represented by the
     * given pixel coordinates, if that pixel is within the chart region described by
     * [.content]. If the point is found, the "dest" argument is set to the point and
     * this function returns true. Otherwise, this function returns false and "dest" is unchanged.
     */
    private fun hitTest(x: Float, y: Float, dest: PointF): Boolean {
        if (!content.contains(x, y)) {
            return false
        }

        dest.set(
                viewport.left + viewport.width() * (x - content.left) / content.width(),
                viewport.top + viewport.height() * (y - content.bottom) / -content.height())
        return true
    }

    private fun fling(velocityX: Int, velocityY: Int) {
        // FIXME fling only works when on the edge of the World (might have something to do with the type of content values - Float, was Int)
        releaseEdgeEffects()
        // Flings use math in pixels (as opposed to math based on the viewport).
        computeScrollSurfaceSize(mSurfaceSizeBuffer)
        mScrollerStartViewport.set(viewport)
        val startX = (mSurfaceSizeBuffer.x * (mScrollerStartViewport.left - content.left) / (content.right - content.left)).toInt()
        val startY = (mSurfaceSizeBuffer.y * (content.bottom - mScrollerStartViewport.bottom) / (content.bottom - content.top)).toInt()
        mScroller.forceFinished(true)
        mScroller.fling(
                startX,
                startY,
                velocityX,
                velocityY,
                0, mSurfaceSizeBuffer.x - content.width().toInt(),
                0, mSurfaceSizeBuffer.y - content.height().toInt(),
                content.width().toInt() / 2,
                content.height().toInt() / 2)
        ViewCompat.postInvalidateOnAnimation(view)
    }

    // Sets up interactions
    private val scaleGestureDetector: ScaleGestureDetector = ScaleGestureDetector(view.context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        /**
         * This is the active focal point in terms of the viewport. Could be a local
         * variable but kept here to minimize per-frame allocations.
         */
        private val viewportFocus = PointF()
        private var lastSpan: Float = 0F

        override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
            lastSpan = detector.currentSpan
            return true
        }

        override fun onScale(detector: ScaleGestureDetector): Boolean {
            val span: Float = detector.currentSpan

            val spanRatio = lastSpan / span
            val newWidth: Float = spanRatio * viewport.width()
            val newHeight: Float = spanRatio * viewport.height()


            if (newWidth < model.worldSizeRestriction || newHeight < model.worldSizeRestriction) return true

            val focusX: Float = detector.focusX
            val focusY: Float = detector.focusY

            // Makes sure that the chart point is within the chart region.
            // See the sample for the implementation of hitTest().
            hitTest(detector.focusX,
                    detector.focusY,
                    viewportFocus)

            val newLeft = viewportFocus.x - newWidth * (focusX - content.left) / content.width()
            val newTop = viewportFocus.y - newHeight * (content.bottom - focusY) / content.height()
            viewport.set(
                    newLeft,
                    newTop,
                    newLeft + newWidth,
                    newTop + newHeight)
            constrainViewport()

            // Invalidates the View to update the display.
            ViewCompat.postInvalidateOnAnimation(view)

            lastSpan = span
            return true
        }
    })


    ////////////////////////////////////////////////////////////////////////////////////////////////
    //
    //     Methods for programmatically changing the viewport
    //
    ////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Sets the current viewport (defined by [.viewport]) to the given
     * X and Y positions. Note that the Y value represents the topmost pixel position, and thus
     * the bottom of the [.viewport] rectangle. For more details on why top and
     * bottom are flipped, see [.viewport].
     */
    fun setViewportBottomLeft(x: Float, y: Float) {
        var newX = x
        var newY = y
        /**
         * Constrains within the scroll range. The scroll range is simply the viewport extremes
         * (content.right, etc.) minus the viewport size. For example, if the extrema were 0 and 10,
         * and the viewport size was 2, the scroll range would be 0 to 8.
         */
        val curWidth = viewport.width()
        val curHeight = viewport.height()
        newX = max(content.left, min(newX, content.right - curWidth))
        newY = max(content.top + curHeight, min(newY, content.bottom))

        viewport.set(newX, newY - curHeight, newX + curWidth, newY)
        model.zoom = viewport.width() / content.width()
    }

    /**
     * Ensures that current viewport is inside the viewport extremes defined by [.content.left],
     * [.content.right], [.content.top] and [.content.bottom].
     */
    fun constrainViewport() {
        // Ensure inside content extremes
        val subMinX = viewport.left - content.left
        if (subMinX < 0) {
            viewport.right -= subMinX
        }
        val subMinY = viewport.top - content.top
        if (subMinY < 0) {
            viewport.bottom -= subMinY
        }
        val superMaxX = viewport.right - content.right
        if (superMaxX > 0) {
            viewport.left -= superMaxX
        }
        val superMaxY = viewport.bottom - content.bottom
        if (superMaxY > 0) {
            viewport.top -= superMaxY
        }
        viewport.left = max(content.left, viewport.left)
        viewport.top = max(content.top, viewport.top)
        viewport.bottom = max(nextUp(viewport.top), min(content.bottom, viewport.bottom))
        viewport.right = max(nextUp(viewport.left), min(content.right, viewport.right))
        model.zoom = viewport.width() / content.width()
    }
}
