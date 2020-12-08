package co.thebabels.schedulecalendarview

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import androidx.recyclerview.widget.RecyclerView
import java.util.*
import kotlin.math.max
import kotlin.math.min

class ScheduleCalendarLayoutManager(context: Context) : RecyclerView.LayoutManager() {

    companion object {
        const val TAG = "SCLayoutManager"
        const val ROWS_COUNT = 24
    }

    var daysCount: Int = 7
    var rowHeight = 200f

    override fun generateDefaultLayoutParams(): RecyclerView.LayoutParams {
        Log.println(
            Log.DEBUG,
            TAG,
            "generateDefaultLayoutParams: ${width}/${daysCount}, ${height}/${ROWS_COUNT}"
        )
        return LayoutParams(width / daysCount, height / ROWS_COUNT)
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
        recycler?.let { detachAndScrapAttachedViews(it) }

        var offset = paddingTop
        for (i in 0 until itemCount) {
            recycler?.let { addChild(i, it) }
        }
    }

    private fun addChild(position: Int, recycler: RecyclerView.Recycler) {
        val v = recycler.getViewForPosition(position)
        addView(v)

        measureChild(v, 0, 0)
        val height = getDecoratedMeasuredHeight(v)
        val left = paddingLeft
        val top = 100 * position // TODO
        val right = left + getDecoratedMeasuredWidth(v)
        val bottom = top + height
        layoutDecorated(v, left, top, right, bottom)
    }

    override fun canScrollVertically(): Boolean {
        return true
    }

    override fun scrollVerticallyBy(
        dy: Int,
        recycler: RecyclerView.Recycler?,
        state: RecyclerView.State?
    ): Int {
        // first child is 'TimeScaleView'
        val firstChild = getChildAt(0) ?: return 0
        val firstTop = getDecoratedTop(firstChild)
        val firstBottom = getDecoratedBottom(firstChild)
        val scrollAmount = calculateVerticalScrollAmount(dy, firstTop, firstBottom)

        Log.d(TAG, "scrollVerticalBy: dy='${dy}' scrollAmount='${scrollAmount}'(firstTop='${firstTop}', firstBottom='${firstBottom}')")

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
        return false
    }

    override fun scrollHorizontallyBy(
        dx: Int,
        recycler: RecyclerView.Recycler?,
        state: RecyclerView.State?
    ): Int {
        return dx
    }

    class LayoutParams : RecyclerView.LayoutParams {

        var start: Date? = null
        var end: Date? = null

        constructor(c: Context?, attrs: AttributeSet?) : super(c, attrs)

        constructor(width: Int, height: Int) : super(width, height)

        constructor(source: MarginLayoutParams?) : super(source)

        constructor(source: ViewGroup.LayoutParams?) : super(source)

        constructor(source: RecyclerView.LayoutParams?) : super(source as ViewGroup.LayoutParams?)
    }


}