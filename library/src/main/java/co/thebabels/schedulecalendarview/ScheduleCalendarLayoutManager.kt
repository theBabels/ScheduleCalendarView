package co.thebabels.schedulecalendarview

import android.content.Context
import android.graphics.PointF
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.SmoothScroller.ScrollVectorProvider
import co.thebabels.schedulecalendarview.extention.*
import co.thebabels.schedulecalendarview.view.TimeScaleView
import java.util.*
import kotlin.math.max
import kotlin.math.min

/**
 * A [RecyclerView.LayoutManager] implementations that lays out schedule items in weekly calendar.
 */
class ScheduleCalendarLayoutManager(context: Context) : RecyclerView.LayoutManager(), ScrollVectorProvider {

    /**
     * Listener for layout event.
     */
    interface Listener {
        fun onFirstItemChanged(position: Int, date: Date)
    }

    companion object {
        const val TAG = "SCLayoutManager"
    }

    var daysCount: Int = 7
    var rowHeight = 0f
    var dateLabelHeight = 0
    var timeScaleWidth = 0
    var currentTimeHeight = 0
    var itemRightPadding = 0
    private lateinit var dateLookUp: DateLookUp
    private var listener: Listener? = null
    private var firstVisibleItemPosition: Int = -1
    private var lastVisibleItemPosition: Int = 0

    // These are scroll positions to be temporarily remembered during re-layout.
    private var tmpVerticalScroll: Int? = null
    private var tmpFirstDateLabelPosition: Int? = null

    private val lastScheduleItemAdapterPosition: Int
        get() = itemCount - 1 - FIX_VIEW_OFFSET

    /**
     * Set [DateLookUp].
     */
    fun setDateLookUp(dateLookUp: DateLookUp) {
        this.dateLookUp = dateLookUp
    }

    /**
     * Set [Listener].
     */
    fun setListener(listener: Listener?) {
        this.listener = listener
    }

    /**
     * Returns a [ScheduleItem.start] of first visible item.
     */
    fun getFirstItemDate(): Date? {
        if (firstVisibleItemPosition < 0) {
            return null
        }
        return dateLookUp.lookUpStart(firstVisibleItemPosition)
    }

    /**
     * Return date at given position.
     */
    fun getDateAt(x: Int, y: Int, minuteSpan: Int = 1): Date? {
        val firstDateLabel = getFirstDateLabel() ?: return null
        val firstDateLabelLP = firstDateLabel.layoutParams as LayoutParams
        val firstDateLabelLeft = getDecoratedLeft(firstDateLabel)
        val timeScale = getTimeScale() ?: return null

        val cal = firstDateLabelLP.start?.toCalendar() ?: return null

        // date (Eliminate the rounding error of the decimal point by moving it by '1' to the right.)
        val dateDiff = (x - firstDateLabelLeft + 1) / rowWidth()
        cal.add(Calendar.DATE, dateDiff)
        cal.clearToMidnight()

        // time
        val minute = (((y - getDecoratedTop(timeScale)) / rowHeight) * 60).toInt().let { min ->
            // If there is a surplus due to an error in converting Float to Int, round it to the nearest value.
            val minSurplus = min % minuteSpan
            if (minSurplus < minuteSpan / 2) {
                min - minSurplus
            } else {
                min + minuteSpan - minSurplus
            }
        }
        cal.add(Calendar.MINUTE, minute)

        Log.v(TAG, "getDateAt: x='${x}', y='${y}', result='${cal.time}', dateDiff='${dateDiff}', minute='${minute}', anchorDate='${firstDateLabelLP.start}', anchorDateLeft='${firstDateLabelLeft}', columnWidth='${rowWidth()}'")
        if (cal.get(Calendar.MINUTE) % 15 > 0) {
            Log.w(TAG, "getDateAt:minute is invalid: x='${x}', y='${y}', result='${cal.time}', dateDiff='${dateDiff}', minute='${minute}', anchorDate='${firstDateLabelLP.start}', anchorDateLeft='${firstDateLabelLeft}', columnWidth='${rowWidth()}'")
        }

        return cal.time
    }

    /**
     * Returns the closest x position from the specified [x] position
     * that is suitable for placing the item.
     */
    fun getValidPositionX(x: Float): Float? {
        val firstDateLabel = getFirstDateLabel() ?: return null
        val firstDateLabelLeft = getDecoratedLeft(firstDateLabel)

        // date (Eliminate the rounding error of the decimal point by moving it by '1' to the right.)
        val dateDiff = (x - firstDateLabelLeft + 1).toInt() / rowWidth()

        return firstDateLabelLeft + (dateDiff * rowWidth()).toFloat()
    }

    /**
     * Returns the closest y position from the specified [y] position
     * that is suitable for placing the item.
     */
    fun getValidPositionY(y: Float, minuteSpan: Int): Float? {
        val timeScale = getTimeScale() ?: return null
        val verticalSpan = rowHeight * minuteSpan / 60f
        val minSpanDiff = ((y - getDecoratedTop(timeScale)) / verticalSpan).toInt()
        return getDecoratedTop(timeScale) + minSpanDiff * verticalSpan
    }

    internal fun getFirstDateLabelX(): Float? {
        return getFirstDateLabel()?.x
    }

    override fun computeScrollVectorForPosition(targetPosition: Int): PointF? {
        if (childCount == 0) {
            return null
        }
        val firstChildPos = getPosition(getChildAt(0)!!)
        val direction = if (targetPosition < firstChildPos) -1f else 1f
        return PointF(direction, 0f)
    }

    override fun generateDefaultLayoutParams(): RecyclerView.LayoutParams {
        Log.println(
                Log.DEBUG,
                TAG,
                "generateDefaultLayoutParams: ${width}/${daysCount}, ${height}/${ROWS_COUNT}"
        )
        return LayoutParams(rowWidth(), height / ROWS_COUNT)
    }

    override fun generateLayoutParams(lp: ViewGroup.LayoutParams?): RecyclerView.LayoutParams {
        return LayoutParams(lp)
    }

    override fun generateLayoutParams(
            c: Context?,
            attrs: AttributeSet?
    ): RecyclerView.LayoutParams {
        return LayoutParams(c, attrs)
    }

    override fun checkLayoutParams(lp: RecyclerView.LayoutParams?): Boolean {
        return lp == null || lp is LayoutParams
    }

    override fun onLayoutChildren(recycler: RecyclerView.Recycler?, state: RecyclerView.State?) {
        Log.v(TAG, "onLayoutChildren: firstVisibleItem='${firstVisibleItemPosition}', lastVisibleItem='${lastVisibleItemPosition}', itemCount='${state?.itemCount}', childCount='${childCount}', '${state}'")
        // FIXME It is better to do a proper redraw.
        if (childCount > 0) {
            // update visible item position because the adapter size is changed.
            firstVisibleItemPosition = getChildAt(0)?.let { getPosition(it) }
                    ?: firstVisibleItemPosition
            lastVisibleItemPosition = getChildAt(childCount - 1 - fixViewCount())?.let { getPosition(it) }
                    ?: lastVisibleItemPosition
            // save the current vertical scroll position until the re-layout is finished.
            tmpVerticalScroll = currentVerticalScroll()
            tmpFirstDateLabelPosition = getFirstDateLabelX()?.toInt()
            Log.v(TAG, "onLayoutChildren: visiblePosition is updated: first='${firstVisibleItemPosition}', last='${lastVisibleItemPosition}', verticalScrollPosition='${tmpVerticalScroll}', tmpFirstDateLabelPosition='${tmpFirstDateLabelPosition}'")
        }
        recycler?.let {
            detachAndScrapAttachedViews(it)
            for (i in max(firstVisibleItemPosition, 0) until itemCount - FIX_VIEW_OFFSET) {
                val right = addChild(i, it, getFirstDateLabel())
                lastVisibleItemPosition = i
                if (right > width - paddingRight && dateLookUp.isDateLabel(i + 1)) {
                    break
                }
            }
            // add TimeScaleView
            addChild(itemCount - 1, recycler, getFirstDateLabel())
            // notify listener
            getFirstItemDate()?.let {
                listener?.onFirstItemChanged(firstVisibleItemPosition, it)
            }
        }

        // remove the temporary vertical scroll position when the layout is finished.
        tmpVerticalScroll = null
        tmpFirstDateLabelPosition = null
    }

    // add child view of item at given adapter [position], and return the view 'right' position.
    private fun addChild(
            position: Int,
            recycler: RecyclerView.Recycler,
            anchorDateLabel: View?
    ): Int {
        Log.v(TAG, "addChild:position='${position}', firstVisibleItem='${firstVisibleItemPosition}', lastVisibleItem='${lastVisibleItemPosition}', itemCount='${itemCount}'")

        val v = recycler.getViewForPosition(position)

        // add view
        if (position > lastVisibleItemPosition) {
            if (position > lastScheduleItemAdapterPosition) {
                addView(v, childCount - (position - (itemCount)))
            } else {
                addView(v, childCount - fixViewCount())
            }
        } else {
            if (position < firstVisibleItemPosition) {
                addView(v, 0)
            } else {
                addView(v, position - firstVisibleItemPosition)
            }
        }

        when (position - (itemCount - 1)) {
            0 -> {
                // the last item is 'TimeScaleView'.
                // assign date to layout param
                val lp = assignDate(v, position)
                lp.width = this.timeScaleWidth
                v.layoutParams = lp

                // measure child
                measureChild(v, 0, 0)
                // calculate layout position based on first
                val left = 0
                val top = headerOffset() + (tmpVerticalScroll ?: 0)
                val right = left + getDecoratedMeasuredWidth(v)
                val bottom = top + getDecoratedMeasuredHeight(v)

                Log.v(
                        TAG, "layoutTimeScale: position='${position}', left='${left}',top='${top}', right='${right}', bottom='${bottom}'"
                )

                // execute layout
                layoutDecoratedWithMargins(v, left, top, right, bottom)
            }
            else -> {
                layoutScheduleItem(v, position, anchorDateLabel)
            }
        }
        return v.right
    }

    override fun smoothScrollToPosition(recyclerView: RecyclerView?, state: RecyclerView.State?, position: Int) {
        val linearSmoothScroller = recyclerView?.let { LinearSmoothScroller(it.context) } ?: return
        linearSmoothScroller.targetPosition = position
        startSmoothScroll(linearSmoothScroller)
    }

    override fun canScrollVertically(): Boolean {
        return true
    }

    override fun scrollVerticallyBy(
            dy: Int,
            recycler: RecyclerView.Recycler?,
            state: RecyclerView.State?
    ): Int {
        // get 'TimeScaleView'
        val timeScaleView = getTimeScale() ?: return 0
        val timeScaleTop = getDecoratedTop(timeScaleView)
        val timeScaleBottom = getDecoratedBottom(timeScaleView)
        val scrollAmount = calculateVerticalScrollAmount(dy, timeScaleTop, timeScaleBottom)

        Log.v(
                TAG,
                "scrollVerticallyBy: dy='${dy}',scrollAmount='${scrollAmount}'(timeScaleTop='${timeScaleTop}',timeScaleBottom='${timeScaleBottom}, height='${height}'')"
        )

        offsetChildrenVertical(-scrollAmount)

        return scrollAmount
    }

    private fun calculateVerticalScrollAmount(
            dy: Int,
            timeScaleTop: Int,
            timeScaleBottom: Int
    ): Int {
        return if (dy > 0) { // upper swipe
            min(dy, timeScaleBottom - height + paddingBottom)
        } else {
            max(dy, -(paddingTop + headerOffset() - timeScaleTop))
        }
    }

    override fun canScrollHorizontally(): Boolean {
        return true
    }

    override fun scrollHorizontallyBy(
            dx: Int,
            recycler: RecyclerView.Recycler?,
            state: RecyclerView.State?
    ): Int {
        // first child of 'DateLabelView'
        val firstView = getFirstDateLabel() ?: return 0
        val lastItem = getChildAt(childCount - 1 - fixViewCount())
        val firstLeft = getDecoratedLeft(firstView)
        val lastRight = lastItem?.let { getDecoratedRight(it) } ?: 0
        val scrollAmount = calculateHorizontalScrollAmount(dx, firstLeft, lastRight)

        val lastItemName = if (lastItem == null) "null" else lastItem::class.java.name
        Log.v(
                TAG,
                "scrollHorizontallyBy:${lastScheduleItemAdapterPosition} dx='${dx}' scrollAmount='${scrollAmount}'(firstLeft='${firstLeft}', lastRight='${lastRight}'${lastItemName})"
        )

        // add & remove new views TODO
        if (dx > 0) { // scroll to right
            recycler?.let {
                // add rows
                if (lastRight - scrollAmount < width - paddingRight) {
                    fillFollowing(scrollAmount, lastRight, it)
                }
                // remove rows
                removeLeftRows(scrollAmount, it)
            }
        } else {
            recycler?.let {
                // add rows
                if (firstLeft - scrollAmount > paddingLeft) {
                    fillPrevious(scrollAmount, lastRight, it)
                }
                // remove rows
                removeRightRows(scrollAmount, it)
            }
        }

        // offset existing views
        offsetChildrenHorizontal(-scrollAmount)

        return scrollAmount
    }

    private fun calculateHorizontalScrollAmount(
            dx: Int,
            firstItemLeft: Int,
            lastItemRight: Int
    ): Int {
        return if (dx > 0) {
            if (lastVisibleItemPosition == lastScheduleItemAdapterPosition) {
                min(dx, lastItemRight - (width - paddingRight))
            } else {
                dx
            }
        } else {
            if (firstVisibleItemPosition == 0) {
                max(dx, -(paddingLeft + timeScaleWidth - firstItemLeft))
            } else {
                dx
            }
        }
    }

    private fun layoutScheduleItem(view: View, position: Int, anchorDateLabel: View?) {
        // assign date to layout param
        val lp = assignDate(view, position)

        // measure child
        lp.height = lp.calcHeight()
        lp.width = lp.calcWidth()
        measureChild(view, 0, 0)

        // calculate layout position based on first
        val isFirstDateLabelInitialization = anchorDateLabel == null
        if (isFirstDateLabelInitialization) {
            firstVisibleItemPosition = max(firstVisibleItemPosition, 0)
            lastVisibleItemPosition = max(lastVisibleItemPosition, 0)
        }
        val rowWidth = rowWidth()
        val anchorDateLabelLp = anchorDateLabel?.layoutParams?.let { it as LayoutParams }
        val anchorDateLabelStart = anchorDateLabelLp?.start
        val left = anchorDateLabel?.let { anchorLabel ->
            getDecoratedRight(anchorLabel) + rowWidth * (lp.start?.dateDiff(anchorDateLabelStart!!)
                    ?.let { it - 1 } ?: 0)
        } ?: tmpFirstDateLabelPosition ?: timeScaleWidth
        val top = if (lp.isDateLabel) {
            0
        } else {
            (lp.start?.hourOfDay()?.let { it * rowHeight + headerOffset() }?.toInt()
                    ?: 0) + currentVerticalScroll()
        }
        val right = left + getDecoratedMeasuredWidth(view)
        val bottom = top + getDecoratedMeasuredHeight(view)
        Log.v(
                TAG,
                "layoutScheduleItem: position='${position}', isDateLabel='${lp.isDateLabel}', left='${left}',top='${top}', right='${right}', bottom='${bottom}', anchorRight='${anchorDateLabel?.let { getDecoratedRight(it) }}', start='${lp.start}',anchorStart='${anchorDateLabelStart}'"
        )

        // execute layout
        layoutDecoratedWithMargins(view, left, top, right, bottom)
    }

    private fun assignDate(view: View, position: Int): LayoutParams {
        val lp = view.layoutParams as LayoutParams
        lp.dateLabelHeight = dateLabelHeight
        lp.rowHeight = rowHeight
        lp.columnWidth = rowWidth()
        lp.itemRightPadding = itemRightPadding
        lp.isDateLabel = dateLookUp.isDateLabel(position)
        lp.isCurrentTime = dateLookUp.isCurrentTime(position)
        lp.currentTimeHeight = currentTimeHeight
        lp.start = dateLookUp.lookUpStart(position)
        lp.end = dateLookUp.lookUpEnd(position)
        lp.isStartSplit = dateLookUp.isStartSplit(position)
        lp.isEndSplit = dateLookUp.isEndSplit(position)
        view.layoutParams = lp
        return lp
    }

    private fun fillFollowing(
            scrollAmount: Int,
            lastItemRight: Int,
            recycler: RecyclerView.Recycler
    ) {
        while (lastVisibleItemPosition < lastScheduleItemAdapterPosition) {
//        for (i in lastVisibleItemPosition + 1 until itemCount - 1 - FIX_VIEW_OFFSET) {
            val right = addChild(lastVisibleItemPosition + 1, recycler, getFirstDateLabel())
            lastVisibleItemPosition++
            if (right > width - paddingRight && dateLookUp.isDateLabel(lastVisibleItemPosition + 1)) {
                break
            }
        }
    }

    private fun fillPrevious(
            scrollAmount: Int,
            firstItemLeft: Int,
            recycler: RecyclerView.Recycler
    ) {
        while (firstVisibleItemPosition > 0) {
            val right = addChild(firstVisibleItemPosition - 1, recycler, getFirstDateLabel())
            firstVisibleItemPosition--
            getFirstItemDate()?.let { listener?.onFirstItemChanged(firstVisibleItemPosition, it) }
            if (right - rowWidth() <= paddingLeft && dateLookUp.isDateLabel(firstVisibleItemPosition)) {
                break
            }
        }
    }

    private fun removeRightRows(
            scrollAmount: Int,
            recycler: RecyclerView.Recycler
    ) {
        while (true) {
            val lastScheduleChildPosition = childCount - 1 - FIX_VIEW_OFFSET
            val left = getChildAt(lastScheduleChildPosition)?.left ?: break
            if (left + scrollAmount > width - paddingRight) {
                removeAndRecycleViewAt(lastScheduleChildPosition, recycler)
                lastVisibleItemPosition--
            } else {
                break
            }
        }
    }

    private fun removeLeftRows(
            scrollAmount: Int,
            recycler: RecyclerView.Recycler
    ) {
        while (true) {
            val right = getChildAt(0)?.right ?: break
            if (right - scrollAmount < paddingLeft) {
                removeAndRecycleViewAt(0, recycler)
                firstVisibleItemPosition++
                getFirstItemDate()?.let {
                    listener?.onFirstItemChanged(firstVisibleItemPosition, it)
                }
            } else {
                break
            }
        }
    }

    private fun getTimeScale(): View? {
        return getChildAt(childCount - 1)?.let {
            if (it is TimeScaleView) it else null
        }
    }

    private fun getFirstDateLabel(): View? {
        // loop all children including fixed-views because a fixed-views may not exist at the time of layout initialization.
        for (i in 0 until childCount) {
            val v = getChildAt(i) ?: return null
            if ((v.layoutParams as LayoutParams).isDateLabel) {
                return v
            }
        }
        return null
    }

    private fun rowWidth(): Int {
        return (width - timeScaleWidth) / daysCount
    }

    private fun fixViewCount(): Int {
        return if (getChildAt(childCount - 1) is TimeScaleView) {
            1
        } else {
            0
        }
    }

    /**
     * Return current vertical scroll distance.
     */
    internal fun currentVerticalScroll(): Int {
        return getTimeScale()?.let { getDecoratedTop(it) - headerOffset() } ?: tmpVerticalScroll
        ?: 0
    }

    private fun headerOffset(): Int {
        return dateLabelHeight
    }

    /**
     * Custom [RecyclerView.LayoutParams] for [ScheduleCalendarLayoutManager].
     */
    class LayoutParams : RecyclerView.LayoutParams {

        var dateLabelHeight: Int = 0
        var currentTimeHeight = 0
        var rowHeight: Float = 0f
        var columnWidth: Int = 0
        var isDateLabel: Boolean = false
        var isCurrentTime: Boolean = false
        var itemRightPadding = 0
        var start: Date? = null
        var end: Date? = null
        var isStartSplit = false
        var isEndSplit = false

        /**
         * true if item is currently selected, otherwise false.
         */
        var isSelected = false

        constructor(c: Context?, attrs: AttributeSet?) : super(c, attrs)

        constructor(width: Int, height: Int) : super(width, height)

        constructor(source: MarginLayoutParams?) : super(source)

        constructor(source: ViewGroup.LayoutParams?) : super(source)

        constructor(source: RecyclerView.LayoutParams?) : super(source as ViewGroup.LayoutParams?)

        fun calcHeight(): Int {
            return if (isDateLabel) {
                dateLabelHeight
            } else if (isCurrentTime) {
                currentTimeHeight
            } else {
                start?.let { start ->
                    end?.hourDiff(start)
                }?.let {
                    // TODO set minimum size
                    max(it * rowHeight, 16f)
                }?.toInt() ?: 0
            }
        }

        fun calcWidth(): Int {
            return if (isDateLabel || isCurrentTime) {
                columnWidth
            } else {
                if (isSelected) {
                    columnWidth
                } else {
                    columnWidth - itemRightPadding
                }
            }
        }
    }

    /**
     * Helper class to look up date of date by adapter position.
     */
    interface DateLookUp {
        fun lookUpStart(position: Int): Date?
        fun lookUpEnd(position: Int): Date?
        fun isDateLabel(position: Int): Boolean
        fun isCurrentTime(position: Int): Boolean
        fun isStartSplit(position: Int): Boolean
        fun isEndSplit(position: Int): Boolean
    }
}