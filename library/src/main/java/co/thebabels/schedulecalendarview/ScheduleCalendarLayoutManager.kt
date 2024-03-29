package co.thebabels.schedulecalendarview

import android.content.Context
import android.graphics.PointF
import android.os.Parcel
import android.os.Parcelable
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.SmoothScroller.ScrollVectorProvider
import co.thebabels.schedulecalendarview.extention.*
import co.thebabels.schedulecalendarview.view.CalendarHeaderMaskView
import co.thebabels.schedulecalendarview.view.CalendarHeaderView
import co.thebabels.schedulecalendarview.view.TimeScaleView
import java.util.*
import kotlin.math.abs
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
    var subColumnMargin = 0
    var headerElevation = 0
    private lateinit var dateLookUp: DateLookUp
    private var listener: Listener? = null
    private var firstVisibleItemPosition: Int = -1
    private var lastVisibleItemPosition: Int = 0

    // [Date.time] value of the item at [firstVisibleItemPosition].
    // This is used to check if the items are not changed between save and restore the state of layout manager.
    private var firstVisibleItemDate: Long? = null

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
     *
     * @param x x position in recycler view
     * @param y y position in recycler view
     * @param minuteSpan span of minute
     * @param allowOverflowFromTimeScale when false is set, if the y position is out of the time scale,
     *  it will be corrected to fit into the position at the edge of the scale.
     */
    fun getDateAt(x: Int, y: Int, minuteSpan: Int = 1, allowOverflowFromTimeScale: Boolean = true): Date? {
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
        }.let {
            if (!allowOverflowFromTimeScale) {
                max(0, min(24 * 60, it))
            } else it
        }
        cal.add(Calendar.MINUTE, minute)

        Log.v(
                TAG,
                "getDateAt: x='${x}', y='${y}', result='${cal.time}', dateDiff='${dateDiff}', minute='${minute}', anchorDate='${firstDateLabelLP.start}', anchorDateLeft='${firstDateLabelLeft}', columnWidth='${rowWidth()}'"
        )
        if (cal.get(Calendar.MINUTE) % 15 > 0) {
            Log.w(
                    TAG,
                    "getDateAt:minute is invalid: x='${x}', y='${y}', result='${cal.time}', dateDiff='${dateDiff}', minute='${minute}', anchorDate='${firstDateLabelLP.start}', anchorDateLeft='${firstDateLabelLeft}', columnWidth='${rowWidth()}'"
            )
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
     *
     * @param y y position in recycler view
     * @param minuteSpan span of minute
     * @param allowNegative If false, this method will not return values before midnight.
     */
    fun getValidPositionY(y: Float, minuteSpan: Int, allowNegative: Boolean = true): Float? {
        val timeScale = getTimeScale() ?: return null
        val verticalSpan = rowHeight * minuteSpan / 60f
        val minSpanDiff = ((y - getDecoratedTop(timeScale)) / verticalSpan).toInt()
        val timeScaleTop = getDecoratedTop(timeScale).toFloat()
        val positionY = timeScaleTop + minSpanDiff * verticalSpan
        return if (allowNegative) positionY else max(positionY, timeScaleTop)
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
            Log.d(TAG, "onLayoutChildren: visiblePosition is updated: first='${firstVisibleItemPosition}', last='${lastVisibleItemPosition}', verticalScrollPosition='${tmpVerticalScroll}', tmpFirstDateLabelPosition='${tmpFirstDateLabelPosition}'")
        }

        // validate params: Initialize the item positions and scroll positions if
        // - the visible item positions are greater than the number of items
        // - the date of item at [firstVisibleItemPosition] differs between the saved one and the current one.
        // These can happen when the [LayoutManager] restored the position by [LayoutManager.onRestoreSavedState], but the [Adapter] did not restore the items.
        val shouldInitLayoutPositions = firstVisibleItemPosition > itemCount ||
                lastVisibleItemPosition > itemCount ||
                (firstVisibleItemDate?.let { date ->
                    dateLookUp.lookUpStart(firstVisibleItemPosition)?.time != date
                } ?: false)
        if (shouldInitLayoutPositions) {
            Log.d(TAG, "onLayoutChildren: visible item position is fixed: first='${firstVisibleItemPosition}'->-1, last='${lastVisibleItemPosition}'->0, firstDate='${firstVisibleItemDate}'")
            firstVisibleItemPosition = -1
            lastVisibleItemPosition = 0
            tmpVerticalScroll = null
            tmpFirstDateLabelPosition = null
        }
        firstVisibleItemDate = null


        recycler?.let {
            detachAndScrapAttachedViews(it)
            for (i in max(firstVisibleItemPosition, 0) until itemCount - FIX_VIEW_OFFSET) {
                val right = addChild(i, it, getFirstDateLabel())
                lastVisibleItemPosition = i
                if (right > width - paddingRight && dateLookUp.isDateLabel(i + 1)) {
                    break
                }
            }
            // add fix views
            for (i in itemCount - FIX_VIEW_OFFSET until itemCount) {
                addChild(i, recycler, getFirstDateLabel())
            }
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
        Log.v(
                TAG,
                "addChild:position='${position}', firstVisibleItem='${firstVisibleItemPosition}', lastVisibleItem='${lastVisibleItemPosition}', itemCount='${itemCount}'"
        )

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

        when ((itemCount - 1) - position) {
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
                        TAG,
                        "layoutTimeScale: position='${position}', left='${left}',top='${top}', right='${right}', bottom='${bottom}'"
                )

                // execute layout
                layoutDecoratedWithMargins(v, left, top, right, bottom)
            }
            1 -> {
                // the second view from the end is 'HeaderView'
                val lp = assignDate(v, position)
                lp.isHeader = true
                lp.width = width
                lp.height = headerOffset()
                v.layoutParams = lp
                lp.calcElevation()?.let { v.elevation = it }

                // measure child
                measureChild(v, 0, 0)
                layoutDecoratedWithMargins(v, 0, 0, getDecoratedMeasuredWidth(v), getDecoratedMeasuredHeight(v))
            }
            2 -> {
                // the third view from the end is 'HeaderMaskView'
                val lp = assignDate(v, position)
                lp.isHeaderMask = true
                lp.width = lp.timeScaleWidth
                lp.height = headerOffset()
                v.layoutParams = lp
                lp.calcElevation()?.let { v.elevation = it }

                // measure child
                measureChild(v, 0, 0)
                layoutDecoratedWithMargins(v, 0, 0, getDecoratedMeasuredWidth(v), getDecoratedMeasuredHeight(v))
            }
            else -> {
                layoutScheduleItem(v, position, anchorDateLabel)
            }
        }
        return v.right
    }

    override fun smoothScrollToPosition(
            recyclerView: RecyclerView?,
            state: RecyclerView.State?,
            position: Int
    ) {
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
        //　limits the scrolling to either vertical or horizontal at the same time.
        // FIXME this may have undesirable side effects on the scrolling process by SmoothScrollTo and SmoothScroller.
        if (state != null && abs(state.remainingScrollVertical) < abs(state.remainingScrollHorizontal)) {
            return 0
        }
        // get 'TimeScaleView'
        val timeScaleView = getTimeScale() ?: return 0
        val timeScaleTop = getDecoratedTop(timeScaleView)
        val timeScaleBottom = getDecoratedBottom(timeScaleView)
        val scrollAmount = calculateVerticalScrollAmount(dy, timeScaleTop, timeScaleBottom)

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

    override fun computeVerticalScrollRange(state: RecyclerView.State): Int {
        val timeScaleView = getTimeScale() ?: return 0
        return timeScaleView.height - (height - paddingTop - paddingBottom - headerOffset())
    }

    override fun computeVerticalScrollOffset(state: RecyclerView.State): Int {
        val timeScaleView = getTimeScale() ?: return 0
        val timeScaleTop = getDecoratedTop(timeScaleView)
        return paddingTop + headerOffset() - timeScaleTop
    }

    override fun computeVerticalScrollExtent(state: RecyclerView.State): Int {
        val range = computeVerticalScrollRange(state)
        val ratio = (height - paddingTop - paddingBottom - headerOffset().toFloat()) / range
        return if (0 < ratio && ratio < 1) (ratio * range).toInt() else range
    }

    override fun canScrollHorizontally(): Boolean {
        return true
    }

    override fun scrollHorizontallyBy(
            dx: Int,
            recycler: RecyclerView.Recycler?,
            state: RecyclerView.State?
    ): Int {
        //　limits the scrolling to either vertical or horizontal at the same time.
        // FIXME this may have undesirable side effects on the scrolling process by SmoothScrollTo and SmoothScroller.
        if (state != null && abs(state.remainingScrollVertical) > abs(state.remainingScrollHorizontal)) {
            return 0
        }
        // first child of 'DateLabelView'
        val firstView = getFirstDateLabel() ?: return 0
        val lastItem = getChildAt(childCount - 1 - fixViewCount())
        val firstLeft = getDecoratedLeft(firstView)
        val lastRight = lastItem?.let { getDecoratedRight(it) } ?: 0
        val scrollAmount = calculateHorizontalScrollAmount(dx, firstLeft, lastRight)

        val lastItemName = if (lastItem == null) "null" else lastItem::class.java.name

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

        // set elevation
        lp.calcElevation()?.let { view.elevation = it }

        // calculate layout position based on first
        val isFirstDateLabelInitialization = anchorDateLabel == null
        if (isFirstDateLabelInitialization) {
            firstVisibleItemPosition = max(firstVisibleItemPosition, 0)
            lastVisibleItemPosition = max(lastVisibleItemPosition, 0)
        }
        val rowWidth = rowWidth()
        val anchorDateLabelLp = anchorDateLabel?.layoutParams?.let { it as LayoutParams }
        val anchorDateLabelStart = anchorDateLabelLp?.start
        val leftAnchor = anchorDateLabel?.let { anchorLabel ->
            getDecoratedRight(anchorLabel) + rowWidth * (lp.start?.dateDiff(anchorDateLabelStart!!)
                    ?.let { it - 1 } ?: 0)
        } ?: tmpFirstDateLabelPosition ?: timeScaleWidth
        val left = if (lp.isSelected || lp.isFillItem) leftAnchor else leftAnchor + (lp.subColumnPosition * (lp.subColumnWidth() + lp.subColumnMargin))
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
                "layoutScheduleItem: position='${position}', isDateLabel='${lp.isDateLabel}', left='${left}',top='${top}', right='${right}', bottom='${bottom}', anchorRight='${
                    anchorDateLabel?.let {
                        getDecoratedRight(
                                it
                        )
                    }
                }', start='${lp.start}',anchorStart='${anchorDateLabelStart}'"
        )

        // execute layout
        layoutDecoratedWithMargins(view, left, top, right, bottom)
    }


    override fun onSaveInstanceState(): Parcelable? {

        return SavedState(
                currentVerticalScroll(),
                getFirstDateLabel()?.x?.toInt() ?: 0,
                firstVisibleItemPosition,
                lastVisibleItemPosition,
                dateLookUp.lookUpStart(firstVisibleItemPosition)?.time ?: 0,
        )
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        if (state is SavedState) {
            tmpVerticalScroll = state.verticalScrollPosition
            tmpFirstDateLabelPosition = state.firstDateLabelPosition
            firstVisibleItemPosition = state.firstVisibleItemPosition
            lastVisibleItemPosition = state.lastVisibleItemPosition
            firstVisibleItemDate = state.firstVisibleItemDate
        }
        super.onRestoreInstanceState(state)
    }

    private fun assignDate(view: View, position: Int): LayoutParams {
        val lp = view.layoutParams as LayoutParams
        lp.dateLabelHeight = dateLabelHeight
        lp.rowHeight = rowHeight
        lp.columnWidth = rowWidth()
        lp.timeScaleWidth = timeScaleWidth
        lp.headerElevation = headerElevation
        lp.itemRightPadding = itemRightPadding
        lp.isDateLabel = dateLookUp.isDateLabel(position)
        lp.isCurrentTime = dateLookUp.isCurrentTime(position)
        lp.isHeader = false
        lp.currentTimeHeight = currentTimeHeight
        lp.start = dateLookUp.lookUpStart(position)
        lp.end = dateLookUp.lookUpEnd(position)
        lp.isStartSplit = dateLookUp.isStartSplit(position)
        lp.isEndSplit = dateLookUp.isEndSplit(position)
        lp.isFillItem = dateLookUp.isFillItem(position)
        val overlapInfo = dateLookUp.lookUpOverlap(position)
        val headOverlap = overlapInfo.headPosition?.let { dateLookUp.lookUpOverlap(it) }
        lp.subColumnPosition = overlapInfo.columnPosition()
        if (headOverlap == null) {
            lp.subColumnCount = overlapInfo.maxDuplicationCount
            lp.subColumnSpan = 1
        } else {
            lp.subColumnCount = headOverlap.maxDuplicationCount
            lp.subColumnSpan = 1
        }
        lp.subColumnMargin = subColumnMargin
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
        return findFixView { it is TimeScaleView }
    }

    private fun getCalendarHeader(): View? {
        return findFixView { it is CalendarHeaderView }
    }

    private fun getCalendarHeaderMaskView(): View? {
        return findFixView { it is CalendarHeaderMaskView }
    }

    private fun findFixView(f: (View) -> Boolean): View? {
        for (i in childCount - 1 downTo childCount - 1 - FIX_VIEW_OFFSET) {
            getChildAt(i)?.let {
                if (f(it)) {
                    return it
                }
            }
        }
        return null
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
        var count = 0
        if (getTimeScale() != null) {
            count += 1
        }
        if (getCalendarHeader() != null) {
            count += 1
        }
        if (getCalendarHeaderMaskView() != null) {
            count += 1
        }
        return count
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
        var timeScaleWidth: Int = 0
        var headerElevation: Int = 0
        var isDateLabel: Boolean = false
        var isCurrentTime: Boolean = false
        var isHeader: Boolean = false
        var isHeaderMask: Boolean = false
        var isFillItem: Boolean = false
        var itemRightPadding = 0
        var subColumnMargin = 0
        var start: Date? = null
        var end: Date? = null
        var isStartSplit = false
        var isEndSplit = false
        var subColumnPosition = 0
        var subColumnCount = 1
        var subColumnSpan = 1

        /**
         * true if item is currently selected, otherwise false.
         */
        var isSelected = false

        constructor(c: Context?, attrs: AttributeSet?) : super(c, attrs)

        constructor(width: Int, height: Int) : super(width, height)

        constructor(source: MarginLayoutParams?) : super(source)

        constructor(source: ViewGroup.LayoutParams?) : super(source)

        constructor(source: RecyclerView.LayoutParams?) : super(source as ViewGroup.LayoutParams?)

        fun calcElevation(): Float? {
            return if (isHeader) {
                headerElevation.toFloat()
            } else if (isDateLabel || isHeaderMask) {
                headerElevation + 1f
            } else {
                null
            }
        }

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
                if (isSelected || isFillItem) {
                    columnWidth
                } else {
                    subColumnWidth() * subColumnSpan
                }
            }
        }

        fun subColumnWidth(): Int {
            return (columnWidth - itemRightPadding - (subColumnCount - 1) * subColumnMargin) / subColumnCount
        }
    }

    /**
     * Information about the overlap of a schedule item with other items.
     */
    data class OverlapInfo(
            /**
             * A list of the positions of duplicate items that precede the item in the list.
             */
            val beforePositions: List<Int>,
            /**
             * Adapter position that is head of the sub column.
             */
            val headPosition: Int?,

            /**
             * Maximum number of duplicate items at the same time.
             */
            val maxDuplicationCount: Int,
    ) {
        /**
         * Returns the sub-column position to layout based on this overlap information
         */
        fun columnPosition(): Int {
            return if (headPosition == null) {
                0
            } else {
                beforePositions.filter { it >= headPosition }.size
            }
        }
    }

    /**
     * Helper class to look up date of date by adapter position.
     */
    interface DateLookUp {
        fun lookUpStart(position: Int): Date?
        fun lookUpEnd(position: Int): Date?
        fun lookUpOverlap(position: Int): OverlapInfo
        fun isDateLabel(position: Int): Boolean
        fun isCurrentTime(position: Int): Boolean
        fun isStartSplit(position: Int): Boolean
        fun isEndSplit(position: Int): Boolean
        fun isFillItem(position: Int): Boolean
    }

    /**
     * Saved state of the layout manager.
     */
    data class SavedState(
            var verticalScrollPosition: Int,
            var firstDateLabelPosition: Int,
            var firstVisibleItemPosition: Int,
            var lastVisibleItemPosition: Int,
            var firstVisibleItemDate: Long,
    ) : Parcelable {

        companion object {
            @JvmField
            val CREATOR: Parcelable.Creator<SavedState> = object : Parcelable.Creator<SavedState> {

                override fun createFromParcel(p: Parcel): SavedState? {
                    return SavedState(p.readInt(), p.readInt(), p.readInt(), p.readInt(), p.readLong())
                }

                override fun newArray(size: Int): Array<SavedState?> {
                    return arrayOfNulls(size)
                }
            }
        }

        override fun describeContents(): Int {
            return 0
        }

        override fun writeToParcel(dest: Parcel?, flags: Int) {
            dest?.writeInt(verticalScrollPosition)
            dest?.writeInt(firstDateLabelPosition)
            dest?.writeInt(firstVisibleItemPosition)
            dest?.writeInt(lastVisibleItemPosition)
            dest?.writeLong(firstVisibleItemDate)
        }
    }
}