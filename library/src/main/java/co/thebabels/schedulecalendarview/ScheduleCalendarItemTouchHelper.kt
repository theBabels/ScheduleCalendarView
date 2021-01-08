package co.thebabels.schedulecalendarview

import android.animation.Animator
import android.animation.ValueAnimator
import android.graphics.Canvas
import android.graphics.Rect
import android.util.Log
import android.view.*
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.animation.Interpolator
import androidx.core.view.GestureDetectorCompat
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.ItemTouchHelper.ViewDropHandler
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.OnItemTouchListener
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import java.util.*
import kotlin.math.max

/**
 * This is a utility class to add editing schedule items support to [ScheduleCalendarRecyclerView].
 * This is based on the implementation of [androidx.recyclerview.widget.ItemTouchHelper].
 */
class ScheduleCalendarItemTouchHelper(val callback: Callback) : RecyclerView.ItemDecoration(),
        RecyclerView.OnChildAttachStateChangeListener {

    companion object {
        private const val TAG = "SCItemTouchHelperCBK"

        /**
         * [ScheduleCalendarItemTouchHelper] is in idle state. At this state, either there is no related motion event by
         * the user or latest motion events have not yet triggered a drag or edit.
         */
        const val ACTION_STATE_IDLE = 0

        /**
         * A View is currently being dragged.
         */
        const val ACTION_STATE_SELECT = 1

        /**
         * A View is currently being dragged.
         */
        const val ACTION_STATE_DRAG = 2

        /**
         * The top edge of a view is currently being dragged .
         */
        const val ACTION_STATE_DRAG_START = 3

        /**
         * Thee bottom edge of a view is currently being dragged.
         */
        const val ACTION_STATE_DRAG_END = 4

        private const val ACTIVE_POINTER_ID_NONE = -1

        private const val EdgeScrollRatio = 0.2f

        /**
         * Returns true given [x] and [y] is on the [child] view.
         *
         * @param child child view
         * @param x touch position in x-coordinate
         * @param y touch position in y-coordinate
         * @param left current left position that the [child] is drawn at
         * @param top current top position that the [child] is drawn at
         */
        private fun hitTest(child: View, x: Float, y: Float, left: Float, top: Float): Boolean {
            return x >= left && x <= left + child.width && y >= top && y <= top + child.height
        }
    }

    /**
     * Re-use array to calculate dx dy for a ViewHolder
     */
    private val tmpPosition = FloatArray(2)

    /**
     * Currently selected view holder
     */
    var selected: RecyclerView.ViewHolder? = null

    /**
     * The reference coordinates for the action start. For drag & drop, this is the time long
     * press is completed vs for swipe, this is the initial touch point.
     */
    var initialTouchX = 0f

    var initialTouchY = 0f

    /**
     * The diff between the last event and initial touch.
     */
    private var dx = 0f

    private var dy = 0f

    /**
     * The coordinates of the selected view at the time it is selected. We record these values
     * when action starts so that we can consistently position it even if LayoutManager moves the
     * View.
     */
    private var selectedStartX = 0f

    private var selectedStartY = 0f

    private var selectedEndY = 0f

    /**
     * The pointer we are tracking.
     */
    var mActivePointerId = ACTIVE_POINTER_ID_NONE

    /**
     * Current mode.
     */
    private var actionState = ACTION_STATE_IDLE

    /**
     * When a View is dragged or swiped and needs to go back to where it was, we create a Recover
     * Animation and animate it to its location using this custom Animator, instead of using
     * framework Animators.
     * Using framework animators has the side effect of clashing with ItemAnimator, creating
     * jumpy UIs.
     */
    private var recoverAnimations: MutableList<RecoverAnimation> = mutableListOf()

    private var recyclerView: RecyclerView? = null

    /**
     * Flag to determine whether or not to attempt to create a new item when a single tap occurs.
     */
    private var createWhenSingleTap = false

    /**
     * The position of the item being newly created.
     * This value is held from the time the item is added to the adapter by user touching the blank space
     * until the selection is released.
     */
    private var createdPosition: Int? = null


    /**
     * When user drags a view to the edge, we start scrolling the LayoutManager as long as View
     * is partially out of bounds.
     */
    /* synthetic access */ val scrollRunnable: Runnable = object : Runnable {
        override fun run() {
            Log.v(TAG, "ScrollRunnable")
            selected?.let { selected ->
                if (scrollIfNecessary()) {
                    // move selected item because it might be lost during scrolling
                    moveIfNecessary(selected)
                }
                recyclerView?.let { recyclerView ->
                    recyclerView.removeCallbacks(this)
                    ViewCompat.postOnAnimation(recyclerView, this)
                }
            }
        }
    }

    /**
     * Used to detect long press.
     */
    private var gestureDetector: GestureDetectorCompat? = null

    /**
     * Callback for when long press occurs.
     */
    private var mItemTouchHelperGestureListener: ItemTouchHelperGestureListener? = null

    private val onItemTouchListener: OnItemTouchListener = object : OnItemTouchListener {

        private val EdgeTouchDetectionRatio = 0.1f
        private var cachedEdgeTouchSize: Int? = null

        override fun onInterceptTouchEvent(
                recyclerView: RecyclerView,
                event: MotionEvent
        ): Boolean {
            Log.v(TAG, "onInterceptTouchEvent: x:'${event.x}', y:'${event.y}', '${event}'")
            gestureDetector?.onTouchEvent(event)
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    mActivePointerId = event.getPointerId(0)
                    initialTouchX = event.x
                    initialTouchY = event.y
                    createWhenSingleTap = selected == null
                    selected?.let { selected ->
                        // start editing based on the tapped position.
                        checkStartDragging(
                                selected.itemView,
                                event.x.toInt(),
                                event.y.toInt()
                        )?.let { nextActionState ->
                            // select with action state
                            Log.v(
                                    TAG,
                                    "onInterceptTouchEvent: start dragging: nextActionState='${nextActionState}'"
                            )
                            select(selected, nextActionState)
                        } ?: run {
                            // if touched location is outside of selected view, clear selection.
                            select(null, ACTION_STATE_IDLE)
                        }
                    } ?: {
                        // TODO recover animation?
//                    val animation: RecoverAnimation = findAnimation(event)
//                    if (animation != null) {
//                        mInitialTouchX -= animation.mX
//                        mInitialTouchY -= animation.mY
//                        endRecoverAnimation(animation.mViewHolder, true)
//                        if (mPendingCleanup.remove(animation.mViewHolder.itemView)) {
//                            mCallback.clearView(mRecyclerView, animation.mViewHolder)
//                        }
//                        select(animation.mViewHolder, animation.mActionState)
//                        updateDxDy(event, mSelectedFlags, 0)
//                    }
                    }
                }
                MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_UP -> {
                    mActivePointerId = ACTIVE_POINTER_ID_NONE
                }
            }
            return selected != null
        }

        /**
         * Returns the next action state based on the selected [view] and location information x, y of a touch event.
         * Null will be returned if touched location is out of the [view].
         *
         * @param view  view of [ScheduleCalendarItemTouchHelper.selected]
         * @param x x location of a touch event
         * @param y y location of a touch event
         * @return one of [ACTION_STATE_DRAG], [ACTION_STATE_DRAG_START], [ACTION_STATE_DRAG_END] or null. null means touched location is out of the [view].
         */
        private fun checkStartDragging(view: View, x: Int, y: Int): Int? {
            if (cachedEdgeTouchSize == null) {
                cachedEdgeTouchSize = view.resources.getDimensionPixelSize(R.dimen.edge_touch_size)
            }
            return cachedEdgeTouchSize?.let { offset ->
                if (y < view.top - offset || y > view.bottom + offset || x < view.left - offset || x > view.right + offset) {
                    null
                } else {
                    val innerOffset =
                            if (view.height > offset * 5) offset else max(view.height - (offset * 3), 0)
                    val lp = view.layoutParams as ScheduleCalendarLayoutManager.LayoutParams
                    if (y < view.top + innerOffset && !lp.isStartSplit) {
                        ACTION_STATE_DRAG_START
                    } else if (y > view.bottom - innerOffset && !lp.isEndSplit) {
                        ACTION_STATE_DRAG_END
                    } else {
                        ACTION_STATE_DRAG
                    }
                }
            }
        }

        override fun onTouchEvent(recyclerView: RecyclerView, event: MotionEvent) {
            Log.v(TAG, "onTouchEvent: '${event.x}', '${event.y}', '${event.actionMasked}'")
            gestureDetector?.onTouchEvent(event)
            if (mActivePointerId == ACTIVE_POINTER_ID_NONE) {
                return
            }
            val action = event.actionMasked
            val activePointerIndex = event.findPointerIndex(mActivePointerId)
            val viewHolder: RecyclerView.ViewHolder = selected ?: return
            when (action) {
                MotionEvent.ACTION_MOVE -> {
                    // Find the index of the active pointer and fetch its position
                    if (activePointerIndex >= 0) {
                        when (actionState) {
                            ACTION_STATE_DRAG -> {
                                updateDxDy(event, activePointerIndex)
                                moveIfNecessary(viewHolder)
                                recyclerView.removeCallbacks(scrollRunnable)
                                scrollRunnable.run()
                                recyclerView.invalidate()
                            }
                            ACTION_STATE_DRAG_START,
                            ACTION_STATE_DRAG_END -> {
                                updateDxDy(event, activePointerIndex)
                                moveIfNecessary(viewHolder)
                                recyclerView.removeCallbacks(scrollRunnable)
                                scrollRunnable.run()
                                recyclerView.invalidate()
                            }
                        }
                    }
                }
                MotionEvent.ACTION_CANCEL -> {
                    select(null, ACTION_STATE_IDLE)
                    mActivePointerId = ACTIVE_POINTER_ID_NONE
                }
                MotionEvent.ACTION_UP -> {
                    when (actionState) {
                        ACTION_STATE_DRAG,
                        ACTION_STATE_DRAG_START,
                        ACTION_STATE_DRAG_END -> {
                            // if current state is 'DRAG', back the state to 'SELECT'.
                            Log.d(
                                    TAG,
                                    "return action state to 'SELECT' from '${actionState}': selected='${selected}'"
                            )
                            // move if necessary
                            moveIfNecessary(viewHolder)
                            // TODO clear selection if item is moved
                            select(selected, ACTION_STATE_SELECT)
                        }
                        else -> {
                            // Do not clear selection here to avoid clear selection that has just been selected.
                            mActivePointerId = ACTIVE_POINTER_ID_NONE
                        }
                    }

                }
                MotionEvent.ACTION_POINTER_UP -> {
                    val pointerIndex = event.actionIndex
                    val pointerId = event.getPointerId(pointerIndex)
                    if (pointerId == mActivePointerId) {
                        // This was our active pointer going up. Choose a new
                        // active pointer and adjust accordingly.
                        val newPointerIndex = if (pointerIndex == 0) 1 else 0
                        mActivePointerId = event.getPointerId(newPointerIndex)
                        updateDxDy(event, pointerIndex)
                    }
                }
            }
        }

        override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {
            Log.d(TAG, "onRequestDisallowInterceptTouchEvent: '${disallowIntercept}'")
            if (!disallowIntercept) {
                return
            }
            select(null, ACTION_STATE_IDLE)
        }
    }

    /**
     * Temporary rect instance that is used when we need to lookup Item decorations.
     */
    private var tmpRect: Rect? = null

    /**
     * When user started to drag scroll. Reset when we don't scroll
     */
    private var mDragScrollStartTimeInMs: Long = 0


    /**
     * Attaches the ItemTouchHelper to the provided RecyclerView. If TouchHelper is already
     * attached to a RecyclerView, it will first detach from the previous one. You can call this
     * method with `null` to detach it from the current RecyclerView.
     *
     * @param recyclerView The RecyclerView instance to which you want to add this helper or
     * `null` if you want to remove ItemTouchHelper from the current
     * RecyclerView.
     */
    fun attachToRecyclerView(recyclerView: RecyclerView?) {
        if (this.recyclerView === recyclerView) {
            return  // nothing to do
        }
        if (this.recyclerView != null) {
            destroyCallbacks()
        }
        this.recyclerView = recyclerView
        if (recyclerView != null) {
            setupCallbacks()
        }
    }

    private fun setupCallbacks() {
        val vc = ViewConfiguration.get(recyclerView!!.context)
//        mSlop = vc.scaledTouchSlop
        recyclerView!!.addItemDecoration(this)
        recyclerView!!.addOnItemTouchListener(onItemTouchListener)
        recyclerView!!.addOnChildAttachStateChangeListener(this)
        startGestureDetection()
    }

    private fun destroyCallbacks() {
        recyclerView!!.removeItemDecoration(this)
        recyclerView!!.removeOnItemTouchListener(onItemTouchListener)
        recyclerView!!.removeOnChildAttachStateChangeListener(this)
        // clean all attached
        recyclerView?.let { recyclerView ->
            val recoverAnimSize: Int = recoverAnimations.size
            for (i in recoverAnimSize - 1 downTo 0) {
                val recoverAnimation: RecoverAnimation = recoverAnimations.get(0)
                callback.clearView(recyclerView, recoverAnimation.mViewHolder)
            }
        }
        recoverAnimations.clear()
//        mOverdrawChild = null
//        mOverdrawChildPosition = -1
        stopGestureDetection()
    }

    private fun getSelectedDxDy(outPosition: FloatArray) {
        outPosition[0] = selectedStartX + dx - (selected?.itemView?.left ?: 0)
        outPosition[1] = selectedStartY + dy - (selected?.itemView?.top ?: 0)
    }

    /**
     * Clear selection.
     *
     * @param requestCode code that is passed to [Callback.onSelectionFinished].
     */
    fun clearSelection(requestCode: Int?) {
        select(null, ACTION_STATE_IDLE, requestCode)
    }


    override fun onDraw(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        // we don't know if RV changed something so we should invalidate this index.
//        mOverdrawChildPosition = -1
        var dx = 0f
        var dy = 0f
        if (selected != null) {
            getSelectedDxDy(tmpPosition)
            dx = tmpPosition[0]
            dy = tmpPosition[1]
        }
        callback.onDraw(c, parent, selected, recoverAnimations, actionState, dx, dy)
    }

    override fun onDrawOver(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        var dx = 0f
        var dy = 0f
        if (selected != null) {
            getSelectedDxDy(tmpPosition)
            dx = tmpPosition.get(0)
            dy = tmpPosition.get(1)
        }
        callback.onDrawOver(c, parent, selected, recoverAnimations, actionState, dx, dy)
    }

    /**
     * Starts dragging the given View. Call with null if you want to clear it.
     *
     * @param selected The ViewHolder to drag. Can be null if you want to cancel the
     * current action, but may not be null if actionState is ACTION_STATE_DRAG.
     * @param actionState The type of action
     */
    fun select(selected: RecyclerView.ViewHolder?, actionState: Int, requestCode: Int? = null) {
        Log.d(TAG, "select: selected='${selected}', actionState='${actionState}'")
        if (selected === this.selected && actionState == this.actionState) {
            return
        }
        val recyclerView = recyclerView ?: return

        val changeSelection =
                selected != this.selected && (actionState == ACTION_STATE_SELECT || actionState == ACTION_STATE_IDLE)
        val prevActionState: Int = this.actionState
        // prevent duplicate animations
        selected?.let { endRecoverAnimation(it, true) }
        this.actionState = actionState
        if (actionState == ACTION_STATE_SELECT || actionState == ACTION_STATE_DRAG || actionState == ACTION_STATE_DRAG_START || actionState == ACTION_STATE_DRAG_END) {
            requireNotNull(selected) { "Must pass a ViewHolder when dragging" }

            // we remove after animation is complete. this means we only elevate the last drag
            // child but that should perform good enough as it is very hard to start dragging a
            // new child before the previous one settles.
//            mOverdrawChild = selected.itemView
//            addChildDrawingOrderCallback()
        }
//        val actionStateMask = ((1 shl ItemTouchHelper.DIRECTION_FLAG_COUNT + ItemTouchHelper.DIRECTION_FLAG_COUNT * actionState) - 1)
        var preventLayout = false
        this.selected?.let { prevSelected ->
            // un-select
            updateSelected(prevSelected.itemView, false)
            if (prevSelected.itemView.parent != null) {
                // find where we should animate to
                val targetTranslateX = 0f
                val targetTranslateY = 0f
                val animationType: Int = prevActionState
                getSelectedDxDy(tmpPosition)
                val currentTranslateX: Float = tmpPosition[0]
                val currentTranslateY: Float = tmpPosition[1]
                val rv = object : RecoverAnimation(
                        prevSelected,
                        animationType,
                        prevActionState,
                        currentTranslateX,
                        currentTranslateY,
                        targetTranslateX,
                        targetTranslateY
                ) {
                    override fun onAnimationEnd(animation: Animator) {
                        super.onAnimationEnd(animation)
                        if (mOverridden) {
                            return
                        }
//                        if (swipeDir <= 0) {
                        // this is a drag or failed swipe. recover immediately
                        if (prevSelected != selected) {
                            callback.clearView(recyclerView, prevSelected)
                        }
//                            // full cleanup will happen on onDrawOver
//                        } else {
//                            // wait until remove animation is complete.
//                            mPendingCleanup.add(prevSelected.itemView)
//                            mIsPendingCleanup = true
//                            if (swipeDir > 0) {
//                                // Animation might be ended by other animators during a layout.
//                                // We defer callback to avoid editing adapter during a layout.
//                                postDispatchSwipe(this, swipeDir)
//                            }
//                        }
                        // removed from the list after it is drawn for the last time
//                        if (mOverdrawChild === prevSelected.itemView) {
//                            removeChildDrawingOrderCallbackIfNecessary(prevSelected.itemView)
//                        }
                    }
                }
                val duration: Long = callback.getAnimationDuration(
                        recyclerView, animationType,
                        targetTranslateX - currentTranslateX, targetTranslateY - currentTranslateY
                )
                rv.setDuration(duration)
                recoverAnimations.add(rv)
                rv.start()
                preventLayout = true
                if (changeSelection) {
                    callback.onSelectionFinished(prevSelected, requestCode)
                }
            } else {
                // TODO
//                removeChildDrawingOrderCallbackIfNecessary(prevSelected.itemView)
                callback.clearView(recyclerView, prevSelected)
            }
            this.selected = null
        }
        if (selected != null) {
//            mSelectedFlags = (callback.getAbsoluteMovementFlags(recyclerView, selected) and actionStateMask
//                    shr mActionState * ItemTouchHelper.DIRECTION_FLAG_COUNT)
            // update selected
            updateSelected(selected.itemView, true)
            if (changeSelection) {
                callback.onSelectionChanged(selected)
            }
            this.selectedStartX = selected.itemView.left.toFloat()
            this.selectedStartY = selected.itemView.top.toFloat()
            this.selectedEndY = selected.itemView.bottom.toFloat()
            this.selected = selected
            if (actionState == ACTION_STATE_DRAG || actionState == ACTION_STATE_DRAG_START || actionState == ACTION_STATE_DRAG_END) {
                this.selected?.itemView?.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            }
        }
        val rvParent: ViewParent? = recyclerView.parent
        rvParent?.requestDisallowInterceptTouchEvent(this.selected != null)
        if (!preventLayout) {
            recyclerView.layoutManager?.requestSimpleAnimationsInNextLayout()
        }
        // clear createdPosition
        if (selected?.adapterPosition != createdPosition) {
            createdPosition = null
        }
//        callback.onSelectedChanged(mSelected, mActionState)
        recyclerView.invalidate()
    }

    private fun updateSelected(view: View, selected: Boolean) {
        view.layoutParams?.let { lp ->
            if (lp is ScheduleCalendarLayoutManager.LayoutParams) {
                lp.isSelected = selected
                view.layoutParams = lp
            }
        }
    }

    /**
     * If user drags the view to the edge, trigger a scroll if necessary.
     */
    fun scrollIfNecessary(): Boolean {
        if (selected == null || (actionState != ACTION_STATE_DRAG && actionState != ACTION_STATE_DRAG_START && actionState != ACTION_STATE_DRAG_END)) {
            mDragScrollStartTimeInMs = Long.MIN_VALUE
            return false
        }
        val selected = selected ?: return false
        val recyclerView = recyclerView ?: return false
        val now = System.currentTimeMillis()
        val scrollDuration: Long = if (mDragScrollStartTimeInMs
                == Long.MIN_VALUE
        ) 0 else now - mDragScrollStartTimeInMs
        val lm: RecyclerView.LayoutManager = recyclerView.layoutManager ?: return false
        val tmpRect = this.tmpRect ?: Rect()
        lm.calculateItemDecorationsForChild(selected.itemView, tmpRect)
        var scrollX = 0
        var scrollY = 0
        if (lm.canScrollHorizontally() && actionState == ACTION_STATE_DRAG) {
            // prepare offset to determine that the selected position has reached the edge.
            // This offset is required because the dx can only be moved to the position of the currently displayed date in a calendar.
            val offset = selected.itemView.width
            val curX = (selectedStartX + dx).toInt()
            val leftDiff: Int = curX - tmpRect.left - recyclerView.paddingLeft - offset
            if (dx <= 0 && leftDiff < 0) {
                scrollX = leftDiff
            } else if (dx >= 0) {
                val rightDiff: Int =
                        (curX + selected.itemView.width + tmpRect.right - (recyclerView.width - recyclerView.paddingRight)) + offset
                if (rightDiff > 0) {
                    scrollX = rightDiff
                }
            }
        }
        if (lm.canScrollVertically()) {
            val recyclerHeight = recyclerView.height
            val touchY = (initialTouchY + dy).toInt()
            val curY = (selectedStartY + dy).toInt()
            if (dy < 0) {
                val topDiff: Int = curY - tmpRect.top - recyclerView.paddingTop
                if (touchY < recyclerHeight * EdgeScrollRatio && topDiff < 0) {
                    scrollY = topDiff
                }
            } else if (dy > 0) {
                val bottomDiff: Int = (curY + selected.itemView.height + tmpRect.bottom
                        - (recyclerHeight - recyclerView.paddingBottom))
                if (touchY > recyclerHeight * (1 - EdgeScrollRatio) && bottomDiff > 0) {
                    scrollY = bottomDiff
                }
            }
        }
        if (scrollX != 0) {
            scrollX = callback.interpolateOutOfBoundsScroll(
                    recyclerView,
                    selected.itemView.getWidth(), scrollX,
                    recyclerView.getWidth(), scrollDuration
            )
        }
        if (scrollY != 0) {
            scrollY = callback.interpolateOutOfBoundsScroll(
                    recyclerView,
                    selected.itemView.getHeight(), scrollY,
                    recyclerView.getHeight(), scrollDuration
            )
        }
        if (scrollX != 0 || scrollY != 0) {
            if (mDragScrollStartTimeInMs == Long.MIN_VALUE) {
                mDragScrollStartTimeInMs = now
            }
            recyclerView.scrollBy(scrollX, scrollY)
            return true
        }
        mDragScrollStartTimeInMs = Long.MIN_VALUE
        return false
    }


    /**
     * Checks if we should move the view holder.
     */
    fun moveIfNecessary(viewHolder: RecyclerView.ViewHolder) {
        val recyclerView = this.recyclerView ?: return
        val lm = recyclerView.layoutManager as ScheduleCalendarLayoutManager
        if (recyclerView.isLayoutRequested) {
            return
        }
        when (actionState) {
            ACTION_STATE_DRAG -> {
                // In the original ItemTouchHelper, the threshold is checked and the target to be swapped is selected here,
                // but this implmentation, the item is moved to the current position by directly referencing the LayoutManager.
                val lp =
                        viewHolder.itemView.layoutParams?.let { if (it is ScheduleCalendarLayoutManager.LayoutParams) it else null }
                                ?: return
                val x = (selectedStartX + dx).toInt()
                val y = (selectedStartY + dy).toInt()
                val endY = (selectedEndY + dy).toInt()
                val start = lm.getDateAt(x, y, callback.minuteSpan()) ?: return
                val end = lm.getDateAt(x, endY, callback.minuteSpan()) ?: return
//                val end = start.toCalendar().apply {
//                    add(Calendar.MINUTE, lp.end?.minuteDiff(lp.start!!)?.toInt() ?: 0)
//                }.time

                // skip if nothing changed
                if (lp.start?.equals(start) == true && lp.end?.equals(end) == true) {
                    return
                }
                Log.d(
                        TAG,
                        "DRAG: [${selectedStartX}, ${selectedStartY}], [${dx}, ${dy}], [${start}, ${end}]"
                )
                if (callback.onMove(recyclerView, viewHolder, start, end)) {
//                    // keep target visible
//                    callback.onMoved(recyclerView, viewHolder, fromPosition, target, toPosition, x, y)
                }
            }
            ACTION_STATE_DRAG_START -> {
                val lp =
                        viewHolder.itemView.layoutParams?.let { if (it is ScheduleCalendarLayoutManager.LayoutParams) it else null }
                                ?: return
                val x = selectedStartX.toInt()
                val y = (selectedStartY + dy).toInt()
                val end = lp.end ?: return
                val start = lm.getDateAt(x, y, callback.minuteSpan())?.let {
                    if (it.before(end)) it else end
                } ?: return

                // skip if nothing changed
                if (lp.start?.equals(start) == true && lp.end?.equals(end) == true) {
                    return
                }
                if (callback.onMove(recyclerView, viewHolder, start, end)) {
//                    // keep target visible
//                    callback.onMoved(recyclerView, viewHolder, fromPosition, target, toPosition, x, y)
                }
            }
            ACTION_STATE_DRAG_END -> {
                val lp =
                        viewHolder.itemView.layoutParams?.let { if (it is ScheduleCalendarLayoutManager.LayoutParams) it else null }
                                ?: return
                val x = selectedStartX.toInt()
                val y = (selectedEndY + dy).toInt()
                val start = lp.start ?: return
                val end = lm.getDateAt(x, y, callback.minuteSpan())?.let {
                    if (it.after(start)) it else start
                } ?: return

                // skip if nothing changed
                if (lp.start?.equals(start) == true && lp.end?.equals(end) == true) {
                    return
                }
                if (callback.onMove(recyclerView, viewHolder, start, end)) {
//                    // keep target visible
//                    callback.onMoved(recyclerView, viewHolder, fromPosition, target, toPosition, x, y)
                }
            }
        }
    }

    override fun onChildViewAttachedToWindow(view: View) {
        // Do nothing
        createdPosition?.let { pos ->
            val vh = recyclerView?.getChildViewHolder(view)
            if (vh?.adapterPosition == pos) {
                select(vh, ACTION_STATE_SELECT)
            }
        }
    }

    override fun onChildViewDetachedFromWindow(view: View) {
//        removeChildDrawingOrderCallbackIfNecessary(view)
        val holder: ViewHolder = recyclerView?.getChildViewHolder(view) ?: return
        if (selected != null && holder === selected) {
            Log.v(TAG, "onChildViewDetachedFromWindow: clear selection")
            select(null, ItemTouchHelper.ACTION_STATE_IDLE)
        } else {
            Log.v(TAG, "onChildViewDetachedFromWindow: end recover animation")
            endRecoverAnimation(holder, false) // this may push it into pending cleanup list.
//            if (mPendingCleanup.remove(holder.itemView)) {
            recyclerView?.let { callback.clearView(it, holder) }
//            }
        }
    }

    private fun startGestureDetection() {
        mItemTouchHelperGestureListener = ItemTouchHelperGestureListener()
        gestureDetector =
                GestureDetectorCompat(recyclerView!!.context, mItemTouchHelperGestureListener)
    }

    private fun stopGestureDetection() {
        mItemTouchHelperGestureListener?.doNotReactToSingleTap()
        mItemTouchHelperGestureListener = null
        if (gestureDetector != null) {
            gestureDetector = null
        }
    }

    /**
     * Returns the animation type or 0 if cannot be found.
     */
    private fun endRecoverAnimation(viewHolder: RecyclerView.ViewHolder, override: Boolean) {
        val recoverAnimSize: Int = recoverAnimations.size
        for (i in recoverAnimSize - 1 downTo 0) {
            val anim: RecoverAnimation = recoverAnimations[i]
            if (anim.mViewHolder === viewHolder) {
                anim.mOverridden = anim.mOverridden or override
                if (!anim.mEnded) {
                    anim.cancel()
                }
                recoverAnimations.removeAt(i)
                return
            }
        }
    }

    internal fun findChildView(event: MotionEvent): View? {
        // first check elevated views, if none, then call RV
        val x = event.x
        val y = event.y
        selected?.let { selected ->
            val selectedView: View = selected.itemView
            if (hitTest(selectedView, x, y, selectedStartX + dx, selectedStartY + dy)) {
                return selectedView
            }
        }
        val animView = recoverAnimations.lastOrNull { anim ->
            val view = anim.mViewHolder.itemView
            hitTest(view, x, y, anim.mX, anim.mY)
        }?.mViewHolder?.itemView
        if (animView != null) {
            return animView
        }
        for (i in recoverAnimations.indices.reversed()) {
            val anim: RecoverAnimation = recoverAnimations.get(i)

        }
        return recyclerView?.findChildViewUnder(x, y)
    }

    fun updateDxDy(ev: MotionEvent, pointerIndex: Int) {
        val lm = recyclerView?.layoutManager?.let { it as ScheduleCalendarLayoutManager } ?: return
        val lp =
                selected?.itemView?.layoutParams?.let { it as ScheduleCalendarLayoutManager.LayoutParams }
                        ?: return
        val x = ev.getX(pointerIndex)
        val tmpDy = ev.getY(pointerIndex) - initialTouchY
        dx = lm.getValidPositionX(x)?.let { it - selectedStartX } ?: return
        dy = if (!lp.isStartSplit || !lp.isEndSplit) {
            lm.getValidPositionY(selectedStartY + tmpDy, callback.minuteSpan(), actionState == ACTION_STATE_DRAG)
                    ?.let { it - selectedStartY }
                    ?: return
        } else 0f
        Log.d(TAG, "updateDxDy: dx='${dx}', dy='${dy}'")
    }

    /**
     * This class is the contract between [ScheduleCalendarItemTouchHelper] and your application. It lets you control
     * which touch behaviors are enabled per each ViewHolder and also receive callbacks when user
     * performs these actions.
     */
    abstract class Callback {

        companion object {
            private val sDragScrollInterpolator = Interpolator { t -> t * t * t * t * t }

            private val sDragViewScrollCapInterpolator = Interpolator { t ->
                var t = t
                t -= 1.0f
                t * t * t * t * t + 1.0f
            }

            /**
             * Drag scroll speed keeps accelerating until this many milliseconds before being capped.
             */
            private const val DRAG_SCROLL_ACCELERATION_LIMIT_TIME_MS: Long = 2000
        }

        private var mCachedMaxScrollSpeed = -1

        private var selectedItem: ScheduleItem? = null

        /**
         * Called when item is selected.
         */
        open fun onSelected(adapterPosition: Int) {}

        /**
         * Called when item selection is finished.
         *
         * @param adapterPosition adapter position of the view holder.
         * This can be null if item is deleted.
         * @param prev previous state of the item at the start of selection.
         * @param requestCode code passed from [clearSelection].
         */
        open fun onSelectionFinished(adapterPosition: Int?, prev: ScheduleItem?, requestCode: Int?) {}

        /**
         * Override to create a new schedule item when user touches empty space.
         *
         * @return adapter position of the created item, or null if new item is not created.
         */
        open fun createItem(date: Date): Int? {
            return null
        }

        /**
         * Check whether the specified [viewHolder] is editable or not.
         */
        abstract fun isEditable(viewHolder: RecyclerView.ViewHolder): Boolean

        /**
         * Returns an [ScheduleItem] at [position] in the adapter.
         */
        abstract fun getScheduleItem(position: Int): ScheduleItem?

        /**
         * Called when ItemTouchHelper wants to move the dragged item from its old position to
         * the new position.
         *
         *
         * If this method returns true, ItemTouchHelper assumes `viewHolder` has been moved
         * to the adapter position of `target` ViewHolder
         * ([ ViewHolder#getAdapterPosition()][ViewHolder.getAdapterPosition]).
         *
         *
         * If you don't support drag & drop, this method will never be called.
         *
         * @param recyclerView The RecyclerView to which ItemTouchHelper is attached to.
         * @param viewHolder   The ViewHolder which is being dragged by the user.
         * @param target       The ViewHolder over which the currently active item is being
         * dragged.
         * @return True if the `viewHolder` has been moved to the adapter position of
         * `target`.
         * @see .onMoved
         */
        abstract fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                start: Date,
                end: Date
        ): Boolean

        private fun getMaxDragScroll(recyclerView: RecyclerView): Int {
            if (mCachedMaxScrollSpeed == -1) {
                mCachedMaxScrollSpeed = recyclerView.resources.getDimensionPixelSize(
                        R.dimen.item_touch_helper_max_drag_scroll_per_frame
                )
            }
            return mCachedMaxScrollSpeed
        }

        /**
         * Called when [.onMove] returns true.
         *
         *
         * ItemTouchHelper does not create an extra Bitmap or View while dragging, instead, it
         * modifies the existing View. Because of this reason, it is important that the View is
         * still part of the layout after it is moved. This may not work as intended when swapped
         * Views are close to RecyclerView bounds or there are gaps between them (e.g. other Views
         * which were not eligible for dropping over).
         *
         *
         * This method is responsible to give necessary hint to the LayoutManager so that it will
         * keep the View in visible area. For example, for LinearLayoutManager, this is as simple
         * as calling [LinearLayoutManager.scrollToPositionWithOffset].
         *
         * Default implementation calls [RecyclerView.scrollToPosition] if the View's
         * new position is likely to be out of bounds.
         *
         *
         * It is important to ensure the ViewHolder will stay visible as otherwise, it might be
         * removed by the LayoutManager if the move causes the View to go out of bounds. In that
         * case, drag will end prematurely.
         *
         * @param recyclerView The RecyclerView controlled by the ItemTouchHelper.
         * @param viewHolder   The ViewHolder under user's control.
         * @param fromPos      The previous adapter position of the dragged item (before it was
         * moved).
         * @param target       The ViewHolder on which the currently active item has been dropped.
         * @param toPos        The new adapter position of the dragged item.
         * @param x            The updated left value of the dragged View after drag translations
         * are applied. This value does not include margins added by
         * [RecyclerView.ItemDecoration]s.
         * @param y            The updated top value of the dragged View after drag translations
         * are applied. This value does not include margins added by
         * [RecyclerView.ItemDecoration]s.
         */
        open fun onMoved(
                recyclerView: RecyclerView,
                viewHolder: ViewHolder, fromPos: Int, target: ViewHolder,
                toPos: Int, x: Int, y: Int
        ) {
            val layoutManager = recyclerView.layoutManager
            if (layoutManager is ViewDropHandler) {
                (layoutManager as ViewDropHandler).prepareForDrop(
                        viewHolder.itemView,
                        target.itemView, x, y
                )
                return
            }

            // if layout manager cannot handle it, do some guesswork
            if (layoutManager?.canScrollHorizontally() == true) {
                val minLeft = layoutManager.getDecoratedLeft(target.itemView)
                if (minLeft <= recyclerView.paddingLeft) {
                    recyclerView.scrollToPosition(toPos)
                }
                val maxRight = layoutManager.getDecoratedRight(target.itemView)
                if (maxRight >= recyclerView.width - recyclerView.paddingRight) {
                    recyclerView.scrollToPosition(toPos)
                }
            }
            if (layoutManager?.canScrollVertically() == true) {
                val minTop = layoutManager.getDecoratedTop(target.itemView)
                if (minTop <= recyclerView.paddingTop) {
                    recyclerView.scrollToPosition(toPos)
                }
                val maxBottom = layoutManager.getDecoratedBottom(target.itemView)
                if (maxBottom >= recyclerView.height - recyclerView.paddingBottom) {
                    recyclerView.scrollToPosition(toPos)
                }
            }
        }

        internal fun onDraw(
                c: Canvas,
                parent: RecyclerView,
                selected: RecyclerView.ViewHolder?,
                recoverAnimationList: List<RecoverAnimation>,
                actionState: Int,
                dX: Float,
                dY: Float
        ) {
            recoverAnimationList.forEachIndexed { i, anim ->
                anim.update()
                val count = c.save()
                onChildDraw(c, parent, anim.mViewHolder, anim.mX, anim.mY, anim.mActionState, false)
                c.restoreToCount(count)
            }
            if (selected != null) {
                val count = c.save()
                onChildDraw(c, parent, selected, dX, dY, actionState, true)
                c.restoreToCount(count)
            }
        }

        internal fun onDrawOver(
                c: Canvas,
                parent: RecyclerView,
                selected: RecyclerView.ViewHolder?,
                recoverAnimationList: MutableList<RecoverAnimation>,
                actionState: Int,
                dX: Float,
                dY: Float
        ) {
            val recoverAnimSize = recoverAnimationList.size
            recoverAnimationList.forEach { anim ->
                val count = c.save()
                onChildDrawOver(
                        c, parent, anim.mViewHolder, anim.mX, anim.mY, anim.mActionState,
                        false
                )
                c.restoreToCount(count)
            }
            if (selected != null) {
                val count = c.save()
                onChildDrawOver(c, parent, selected, dX, dY, actionState, true)
                c.restoreToCount(count)
            }
            var hasRunningAnimation = false
            for (i in recoverAnimSize - 1 downTo 0) {
                val anim = recoverAnimationList[i]
                if (anim.mEnded && !anim.mIsPendingCleanup) {
                    recoverAnimationList.removeAt(i)
                } else if (!anim.mEnded) {
                    hasRunningAnimation = true
                }
            }
            if (hasRunningAnimation) {
                parent.invalidate()
            }
        }

        /**
         * Called by the ItemTouchHelper when the user interaction with an element is over and it
         * also completed its animation.
         *
         *
         * This is a good place to clear all changes on the View that was done in
         * [.onSelectedChanged],
         * [.onChildDraw] or
         * [.onChildDrawOver].
         *
         * @param recyclerView The RecyclerView which is controlled by the ItemTouchHelper.
         * @param viewHolder   The View that was interacted by the user.
         */
        open fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
            ItemTouchUIUtilImpl.INSTANCE.clearView(viewHolder.itemView)
        }

        /**
         * Called by ItemTouchHelper on RecyclerView's onDraw callback.
         *
         *
         * If you would like to customize how your View's respond to user interactions, this is
         * a good place to override.
         *
         *
         * Default implementation translates the child by the given `dX`,
         * `dY`.
         * ItemTouchHelper also takes care of drawing the child after other children if it is being
         * dragged. This is done using child re-ordering mechanism. On platforms prior to L, this
         * is
         * achieved via [android.view.ViewGroup.getChildDrawingOrder] and on L
         * and after, it changes View's elevation value to be greater than all other children.)
         *
         * @param c                 The canvas which RecyclerView is drawing its children
         * @param recyclerView      The RecyclerView to which ItemTouchHelper is attached to
         * @param viewHolder        The ViewHolder which is being interacted by the User or it was
         * interacted and simply animating to its original position
         * @param dX                The amount of horizontal displacement caused by user's action
         * @param dY                The amount of vertical displacement caused by user's action
         * @param actionState       The type of interaction on the View. Is either [                          ][.ACTION_STATE_DRAG] or [.ACTION_STATE_SWIPE].
         * @param isCurrentlyActive True if this view is currently being controlled by the user or
         * false it is simply animating back to its original state.
         * @see .onChildDrawOver
         */
        open fun onChildDraw(
                c: Canvas,
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                dX: Float, dY: Float, actionState: Int, isCurrentlyActive: Boolean
        ) {
            ItemTouchUIUtilImpl.INSTANCE.onDraw(
                    c, recyclerView, viewHolder.itemView, dX, dY,
                    actionState, isCurrentlyActive
            )
        }

        /**
         * Called by ItemTouchHelper on RecyclerView's onDraw callback.
         *
         *
         * If you would like to customize how your View's respond to user interactions, this is
         * a good place to override.
         *
         *
         * Default implementation translates the child by the given `dX`,
         * `dY`.
         * ItemTouchHelper also takes care of drawing the child after other children if it is being
         * dragged. This is done using child re-ordering mechanism. On platforms prior to L, this
         * is
         * achieved via [android.view.ViewGroup.getChildDrawingOrder] and on L
         * and after, it changes View's elevation value to be greater than all other children.)
         *
         * @param c                 The canvas which RecyclerView is drawing its children
         * @param recyclerView      The RecyclerView to which ItemTouchHelper is attached to
         * @param viewHolder        The ViewHolder which is being interacted by the User or it was
         * interacted and simply animating to its original position
         * @param dX                The amount of horizontal displacement caused by user's action
         * @param dY                The amount of vertical displacement caused by user's action
         * @param actionState       The type of interaction on the View. Is either [                          ][.ACTION_STATE_DRAG] or [.ACTION_STATE_SWIPE].
         * @param isCurrentlyActive True if this view is currently being controlled by the user or
         * false it is simply animating back to its original state.
         * @see .onChildDrawOver
         */
        open fun onChildDrawOver(
                c: Canvas,
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                dX: Float,
                dY: Float,
                actionState: Int,
                isCurrentlyActive: Boolean
        ) {
            ItemTouchUIUtilImpl.INSTANCE.onDrawOver(
                    c,
                    recyclerView,
                    viewHolder.itemView,
                    dX,
                    dY,
                    actionState,
                    isCurrentlyActive
            )
        }

        /**
         * Called by the ItemTouchHelper when user action finished on a ViewHolder and now the View
         * will be animated to its final position.
         *
         *
         * Default implementation uses ItemAnimator's duration values. If
         * `animationType` is [.ANIMATION_TYPE_DRAG], it returns
         * [RecyclerView.ItemAnimator.getMoveDuration], otherwise, it returns
         * [RecyclerView.ItemAnimator.getRemoveDuration]. If RecyclerView does not have
         * any [RecyclerView.ItemAnimator] attached, this method returns
         * `DEFAULT_DRAG_ANIMATION_DURATION` or `DEFAULT_SWIPE_ANIMATION_DURATION`
         * depending on the animation type.
         *
         * @param recyclerView  The RecyclerView to which the ItemTouchHelper is attached to.
         * @param animationType The type of animation. Is one of [.ANIMATION_TYPE_DRAG],
         * [.ANIMATION_TYPE_SWIPE_CANCEL] or
         * [.ANIMATION_TYPE_SWIPE_SUCCESS].
         * @param animateDx     The horizontal distance that the animation will offset
         * @param animateDy     The vertical distance that the animation will offset
         * @return The duration for the animation
         */
        open fun getAnimationDuration(
                recyclerView: RecyclerView, animationType: Int,
                animateDx: Float, animateDy: Float
        ): Long {
            val itemAnimator = recyclerView.itemAnimator
            return if (itemAnimator == null) {
                if (animationType == ItemTouchHelper.ANIMATION_TYPE_DRAG) ItemTouchHelper.Callback.DEFAULT_DRAG_ANIMATION_DURATION.toLong() else ItemTouchHelper.Callback.DEFAULT_SWIPE_ANIMATION_DURATION.toLong()
            } else {
                if (animationType == ItemTouchHelper.ANIMATION_TYPE_DRAG) itemAnimator.moveDuration else itemAnimator.removeDuration
            }
        }

        /**
         * Called by the ItemTouchHelper when user is dragging a view out of bounds.
         *
         *
         * You can override this method to decide how much RecyclerView should scroll in response
         * to this action. Default implementation calculates a value based on the amount of View
         * out of bounds and the time it spent there. The longer user keeps the View out of bounds,
         * the faster the list will scroll. Similarly, the larger portion of the View is out of
         * bounds, the faster the RecyclerView will scroll.
         *
         * @param recyclerView        The RecyclerView instance to which ItemTouchHelper is
         * attached to.
         * @param viewSize            The total size of the View in scroll direction, excluding
         * item decorations.
         * @param viewSizeOutOfBounds The total size of the View that is out of bounds. This value
         * is negative if the View is dragged towards left or top edge.
         * @param totalSize           The total size of RecyclerView in the scroll direction.
         * @param msSinceStartScroll  The time passed since View is kept out of bounds.
         * @return The amount that RecyclerView should scroll. Keep in mind that this value will
         * be passed to [RecyclerView.scrollBy] method.
         */
        open fun interpolateOutOfBoundsScroll(
                recyclerView: RecyclerView,
                viewSize: Int, viewSizeOutOfBounds: Int,
                totalSize: Int, msSinceStartScroll: Long
        ): Int {
            val maxScroll: Int = getMaxDragScroll(recyclerView)
            val absOutOfBounds = Math.abs(viewSizeOutOfBounds)
            val direction = Math.signum(viewSizeOutOfBounds.toFloat()).toInt()
            // might be negative if other direction
            val outOfBoundsRatio = Math.min(1f, 1f * absOutOfBounds / viewSize)
            val cappedScroll = (direction * maxScroll
                    * sDragViewScrollCapInterpolator.getInterpolation(outOfBoundsRatio)).toInt()
            val timeRatio: Float =
                    if (msSinceStartScroll > DRAG_SCROLL_ACCELERATION_LIMIT_TIME_MS) {
                        1f
                    } else {
                        msSinceStartScroll.toFloat() / DRAG_SCROLL_ACCELERATION_LIMIT_TIME_MS
                    }
            val value = (cappedScroll * sDragScrollInterpolator.getInterpolation(timeRatio)).toInt()
            return if (value == 0) {
                if (viewSizeOutOfBounds > 0) 1 else -1
            } else value
        }

        open fun minuteSpan(): Int {
            return 15
        }

        /**
         * Called when selected view holder is changed.
         */
        fun onSelectionChanged(holder: RecyclerView.ViewHolder) {
            this.selectedItem = getScheduleItem(holder.adapterPosition)
            this.onSelected(holder.adapterPosition)
        }

        /**
         * Called when item selection is finished.
         */
        fun onSelectionFinished(holder: RecyclerView.ViewHolder, requestCode: Int?) {
            val prevItem = this.selectedItem
            // item may be deleted if key is different from selected item.
            val mayBeDeleted = getScheduleItem(holder.adapterPosition)
                    ?.let { it.key() != prevItem?.key() } ?: true
            onSelectionFinished(if (mayBeDeleted) null else holder.adapterPosition, prevItem, requestCode)
            this.selectedItem = null
        }
    }

    private inner class ItemTouchHelperGestureListener : SimpleOnGestureListener() {
        /**
         * Whether to execute code in response to the the invoking of
         * [ItemTouchHelperGestureListener.onLongPress].
         *
         * It is necessary to control this here because
         * [GestureDetector.SimpleOnGestureListener] can only be set on a
         * [GestureDetector] in a GestureDetector's constructor, a GestureDetector will call
         * onLongPress if an [MotionEvent.ACTION_DOWN] event is not followed by another event
         * that would cancel it (like [MotionEvent.ACTION_UP] or
         * [MotionEvent.ACTION_CANCEL]), the long press responding to the long press event
         * needs to be cancellable to prevent unexpected behavior.
         *
         * @see .doNotReactToLongPress
         */
        private var mShouldReactToSingleTap = true

        /**
         * Call to prevent executing code in response to
         * [ItemTouchHelperGestureListener.onLongPress] being called.
         */
        fun doNotReactToSingleTap() {
            mShouldReactToSingleTap = false
        }

        override fun onDown(e: MotionEvent): Boolean {
            return true
        }

        override fun onSingleTapUp(e: MotionEvent?): Boolean {
            Log.v(TAG, "onSingleTapUp: select x:'${e?.x}',y:'${e?.y}', ${e?.actionMasked}")
            if (!mShouldReactToSingleTap) {
                return false
            }

            // skip if event is null
            if (e == null) {
                return false
            }

            val childView = findChildView(e)
            if (childView != null) {
                // select child view
                recyclerView?.getChildViewHolder(childView)?.let { vh ->
                    val pointerId = e.getPointerId(0)
                    // single tap is deferred.
                    // Check w/ active pointer id to avoid selecting after motion event is canceled.
                    if (pointerId == mActivePointerId && callback.isEditable(vh)) {
                        val index = e.findPointerIndex(mActivePointerId)
                        val x = e.getX(index)
                        val y = e.getY(index)
                        initialTouchX = x
                        initialTouchY = y
                        dy = 0f
                        dx = dy
                        Log.d(
                                TAG,
                                "onSingleTapUp: select x:'${initialTouchX}',y:'${initialTouchY}'"
                        )
                        select(vh, ACTION_STATE_SELECT)
                        return true
                    }
                }
            } else if (createWhenSingleTap) {
                // create new item if blank space is touched.
                val pointerId = e.getPointerId(0)
                if (pointerId == mActivePointerId && actionState == ACTION_STATE_IDLE && selected == null) {
                    val index = e.findPointerIndex(mActivePointerId)
                    val x = e.getX(index)
                    val y = e.getY(index)
                    val lm = recyclerView?.layoutManager
                    if (lm != null && lm is ScheduleCalendarLayoutManager) {
                        val createdItemPosition =
                                lm.getDateAt(x.toInt(), y.toInt(), callback.minuteSpan())
                                        ?.let { callback.createItem(it) }
                        // store created position to select it when view holder is attached.
                        if (createdItemPosition != null) {
                            createdPosition = createdItemPosition
                        }
                    }
                }
            }

            return false
        }
    }

    /**
     * @property mViewHolder
     * @property mAnimationType
     * @property mActionState
     * @property mStartDx
     * @property mStartDy
     * @property mTargetX
     * @property mTargetY
     */
    internal open class RecoverAnimation internal constructor(
            val mViewHolder: RecyclerView.ViewHolder,
            val mAnimationType: Int,
            val mActionState: Int,
            val mStartDx: Float,
            val mStartDy: Float,
            val mTargetX: Float,
            val mTargetY: Float,
    ) : Animator.AnimatorListener {
        private val mValueAnimator: ValueAnimator
        internal var mIsPendingCleanup = false
        var mX = 0f
        var mY = 0f

        // if user starts touching a recovering view, we put it into interaction mode again,
        // instantly.
        var mOverridden = false
        var mEnded = false
        private var mFraction = 0f
        fun setDuration(duration: Long) {
            mValueAnimator.duration = duration
        }

        fun start() {
            mViewHolder.setIsRecyclable(false)
            mValueAnimator.start()
        }

        fun cancel() {
            mValueAnimator.cancel()
        }

        fun setFraction(fraction: Float) {
            mFraction = fraction
        }

        /**
         * We run updates on onDraw method but use the fraction from animator callback.
         * This way, we can sync translate x/y values w/ the animators to avoid one-off frames.
         */
        fun update() {
            mX = if (mStartDx == mTargetX) {
                mViewHolder.itemView.translationX
            } else {
                mStartDx + mFraction * (mTargetX - mStartDx)
            }
            mY = if (mStartDy == mTargetY) {
                mViewHolder.itemView.translationY
            } else {
                mStartDy + mFraction * (mTargetY - mStartDy)
            }
        }

        override fun onAnimationStart(animation: Animator) {}
        override fun onAnimationEnd(animation: Animator) {
            if (!mEnded) {
                mViewHolder.setIsRecyclable(true)
            }
            mEnded = true
        }

        override fun onAnimationCancel(animation: Animator) {
            setFraction(1f) //make sure we recover the view's state.
        }

        override fun onAnimationRepeat(animation: Animator) {}

        init {
            mValueAnimator = ValueAnimator.ofFloat(0f, 1f)
            mValueAnimator.addUpdateListener { animation -> setFraction(animation.animatedFraction) }
            mValueAnimator.setTarget(mViewHolder.itemView)
            mValueAnimator.addListener(this)
            setFraction(0f)
        }
    }
}