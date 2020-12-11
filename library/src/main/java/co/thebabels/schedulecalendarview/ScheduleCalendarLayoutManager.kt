package co.thebabels.schedulecalendarview

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import androidx.recyclerview.widget.RecyclerView
import co.thebabels.schedulecalendarview.extention.dateDiff
import co.thebabels.schedulecalendarview.extention.hourDiff
import co.thebabels.schedulecalendarview.extention.hourOfDay
import java.util.*
import kotlin.math.max
import kotlin.math.min

/**
 * A [RecyclerView.LayoutManager] implementations that lays out schedule items in weekly calendar.
 */
class ScheduleCalendarLayoutManager(context: Context) : RecyclerView.LayoutManager() {

    /**
     * Listener for layout event.
     */
    interface Listener {
        fun onFirstItemChanged(position: Int, date: Date)
    }

    companion object {
        const val TAG = "SCLayoutManager"
        const val ROWS_COUNT = 24
        const val FIX_VIEW_OFFSET = 1
    }

    var daysCount: Int = 7
    var rowHeight = 0f
    var dateLabelHeight = 0
    var timeScaleWidth = 0
    private lateinit var dateLookUp: DateLookUp
    private var listener: Listener? = null
    private var firstVisibleItemPosition: Int = 0
    private var lastVisibleItemPosition: Int = 0

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
        if (firstVisibleItemPosition < FIX_VIEW_OFFSET) {
            return null
        }
        return dateLookUp.lookUpStart(firstVisibleItemPosition)
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
        Log.d(TAG, "onLayoutChildren")
        recycler?.let {
            detachAndScrapAttachedViews(it)
            for (i in 0 until itemCount) {
                val right = addChild(i, it, getFirstDateLabel())
                lastVisibleItemPosition = i
                if (right > width - paddingRight && dateLookUp.isDateLabel(i + 1)) {
                    break
                }
            }
            getFirstItemDate()?.let {
                listener?.onFirstItemChanged(firstVisibleItemPosition, it)
            }
        }
    }

    // add child view of item at given adapter [position], and return the view 'right' position.
    private fun addChild(
        position: Int,
        recycler: RecyclerView.Recycler,
        anchorDateLabel: View?
    ): Int {
        val v = recycler.getViewForPosition(position)

        // add view
        if (position <= firstVisibleItemPosition) {
            if (position < FIX_VIEW_OFFSET) {
                addView(v, position)
            } else {
                addView(v, FIX_VIEW_OFFSET)
            }
        } else {
            addView(v)
        }

        if (position == 0) {
            // assign date to layout param
            val lp = assignDate(v, position)
            lp.width = this.timeScaleWidth
            v.layoutParams = lp

            // measure child
            measureChild(v, 0, 0)
            // calculate layout position based on first
            val left = 0
            val top = 0
            val right = left + getDecoratedMeasuredWidth(v) // TODO
            val bottom = top + getDecoratedMeasuredHeight(v)

            Log.d(
                TAG,
                "layoutTimeScale: position='${position}', left='${left}',top='${top}', right='${right}', bottom='${bottom}'"
            )

            // execute layout
            layoutDecoratedWithMargins(v, left, top, right, bottom)
        } else {
            layoutScheduleItem(v, position, anchorDateLabel)
        }
        return v.right
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

        Log.d(
            TAG,
            "scrollVerticalBy: dy='${dy}',scrollAmount='${scrollAmount}'(firstTop='${timeScaleTop}',firstBottom='${timeScaleBottom}')"
        )

        offsetChildrenVertical(-scrollAmount)

        return scrollAmount
    }

    private fun calculateVerticalScrollAmount(
        dy: Int,
        firstItemTop: Int,
        firstItemBottom: Int
    ): Int {
        return if (dy > 0) { // upper swipe
            min(dy, firstItemBottom - height + paddingBottom)
        } else {
            max(dy, -(paddingTop - firstItemTop))
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
        val lastItem = getChildAt(childCount - 1)
        val firstLeft = getDecoratedLeft(firstView)
        val firstRight = getDecoratedRight(firstView)
        val lastRight = lastItem?.let { getDecoratedRight(it) } ?: 0
        val scrollAmount = calculateHorizontalScrollAmount(dx, firstLeft, lastRight)

        Log.d(
            TAG,
            "scrollVerticalBy: dy='${dx}' scrollAmount='${scrollAmount}'(firstTop='${firstLeft}', firstBottom='${firstRight}')"
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
            if (lastVisibleItemPosition == itemCount - 1) {
                min(dx, lastItemRight - (width - paddingRight))
            } else {
                dx
            }
        } else {
            if (firstVisibleItemPosition == FIX_VIEW_OFFSET) {
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
        measureChild(view, 0, 0)

        // calculate layout position based on first
        val isFirstDateLabelInitialization = anchorDateLabel == null
        if (isFirstDateLabelInitialization) {
            firstVisibleItemPosition = FIX_VIEW_OFFSET
        }
        val rowWidth = rowWidth()
        val anchorDateLabelLp = anchorDateLabel?.layoutParams?.let { it as LayoutParams }
        val anchorDateLabelStart = anchorDateLabelLp?.start
        val left = anchorDateLabel?.let { anchorLabel ->
            getDecoratedRight(anchorLabel) + rowWidth * (lp.start?.dateDiff(anchorDateLabelStart!!)
                ?.let { it - 1 } ?: 0)
        } ?: timeScaleWidth
        val top = if (lp.isDateLabel) {
            0
        } else {
            (lp.start?.hourOfDay()?.let { it * rowHeight + dateLabelHeight }?.toInt()
                ?: 0) + currentVerticalScroll()
        }
        val right = left + getDecoratedMeasuredWidth(view)
        val bottom = top + getDecoratedMeasuredHeight(view)
        Log.d(
            TAG,
            "layoutColumn: position='${position}', left='${left}',top='${top}', right='${right}', bottom='${bottom}'"
        )

        // execute layout
        layoutDecoratedWithMargins(view, left, top, right, bottom)
    }

    private fun assignDate(view: View, position: Int): LayoutParams {
        val lp = view.layoutParams as LayoutParams
        lp.dateLabelHeight = dateLabelHeight
        lp.rowHeight = rowHeight
        lp.isDateLabel = dateLookUp.isDateLabel(position)
        lp.start = dateLookUp.lookUpStart(position)
        lp.end = dateLookUp.lookUpEnd(position)
        view.layoutParams = lp
        return lp
    }

    private fun fillFollowing(
        scrollAmount: Int,
        lastItemRight: Int,
        recycler: RecyclerView.Recycler
    ) {
        for (i in lastVisibleItemPosition + 1 until itemCount) {
            val right = addChild(i, recycler, getFirstDateLabel())
            lastVisibleItemPosition++
            if (right > width - paddingRight && dateLookUp.isDateLabel(i + 1)) {
                break
            }
        }
    }

    private fun fillPrevious(
        scrollAmount: Int,
        firstItemLeft: Int,
        recycler: RecyclerView.Recycler
    ) {
        while (firstVisibleItemPosition > FIX_VIEW_OFFSET) {
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
            val left = getChildAt(childCount - 1)?.left ?: break
            if (left + scrollAmount > width - paddingRight) {
                removeAndRecycleViewAt(childCount - 1, recycler)
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
            val right = getChildAt(FIX_VIEW_OFFSET)?.right ?: break
            if (right - scrollAmount < paddingLeft) {
                removeAndRecycleViewAt(FIX_VIEW_OFFSET, recycler)
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
        return getChildAt(0)
    }

    private fun getFirstDateLabel(): View? {
        for (i in FIX_VIEW_OFFSET until childCount) {
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

    /**
     * Return current vertical scroll distance.
     */
    private fun currentVerticalScroll(): Int {
        return getTimeScale()?.let { getDecoratedTop(it) } ?: 0
    }

    /**
     * Custom [RecyclerView.LayoutParams] for [ScheduleCalendarLayoutManager].
     */
    class LayoutParams : RecyclerView.LayoutParams {

        var dateLabelHeight: Int = 0
        var rowHeight: Float = 0f
        var isDateLabel: Boolean = false
        var start: Date? = null
        var end: Date? = null

        constructor(c: Context?, attrs: AttributeSet?) : super(c, attrs)

        constructor(width: Int, height: Int) : super(width, height)

        constructor(source: MarginLayoutParams?) : super(source)

        constructor(source: ViewGroup.LayoutParams?) : super(source)

        constructor(source: RecyclerView.LayoutParams?) : super(source as ViewGroup.LayoutParams?)

        fun calcHeight(): Int {
            return if (isDateLabel) {
                dateLabelHeight
            } else {
                start?.let { start ->
                    end?.hourDiff(start)
                }?.let {
                    it * rowHeight
                }?.toInt() ?: 0
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
    }
}