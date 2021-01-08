package co.thebabels.schedulecalendarview

import android.view.View
import androidx.recyclerview.widget.OrientationHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SnapHelper
import co.thebabels.schedulecalendarview.extention.dayOfWeek
import co.thebabels.schedulecalendarview.extention.toCalendar
import java.util.*
import kotlin.math.abs

/**
 * Implementation of the [SnapHelper] supporting pager style snapping in horizontal orientation.
 *
 * This is based on [androidx.recyclerview.widget.PagerSnapHelper] implementation.
 */
class ScheduleCalendarSnapHelper : SnapHelper() {

    companion object {
        private const val TAG = "SCSnapHelper"
    }

    private var startDayOfWeek = Calendar.MONDAY
    private var recyclerView: RecyclerView? = null
    private var horizontalHelper: OrientationHelper? = null

    /**
     * Set the start day of week.
     * default is [Calendar.MONDAY]
     */
    fun setStartDayOfWeek(dayOfWeek: Int) {
        this.startDayOfWeek = dayOfWeek
    }

    override fun attachToRecyclerView(recyclerView: RecyclerView?) {
        super.attachToRecyclerView(recyclerView)
        this.recyclerView = recyclerView
    }

    override fun calculateDistanceToFinalSnap(layoutManager: RecyclerView.LayoutManager, targetView: View): IntArray? {
        val out = IntArray(2)
        if (layoutManager.canScrollHorizontally()) {
            targetView.layoutParams?.let { lp ->
                if (lp is ScheduleCalendarLayoutManager.LayoutParams) {
                    lp.start?.let { date ->
                        if (isHeadDate(date)) {
                            out[0] = distanceFromHead(layoutManager, targetView, getHorizontalHelper(layoutManager))
                        } else if (isEndDate(date)) {
                            out[0] = distanceFromEnd(layoutManager, targetView, getHorizontalHelper(layoutManager))
                        }
                    }
                } else null
            } ?: run { out[0] = 0 }
        } else {
            out[0] = 0
        }
        out[1] = 0
        return out
    }

    override fun findSnapView(layoutManager: RecyclerView.LayoutManager?): View? {
        return if (layoutManager!!.canScrollHorizontally()) {
            findAnchorView(layoutManager, getHorizontalHelper(layoutManager))
        } else null
    }

    // TODO
    override fun findTargetSnapPosition(layoutManager: RecyclerView.LayoutManager?, velocityX: Int, velocityY: Int): Int {
        val itemCount = layoutManager?.itemCount ?: return RecyclerView.NO_POSITION
        if (itemCount == 0) {
            return RecyclerView.NO_POSITION
        }

        // skip if velocityX is not small enough.
        if (abs(velocityX) < abs(velocityY)) {
            return RecyclerView.NO_POSITION
        }

        val orientationHelper: OrientationHelper = getOrientationHelper(layoutManager)
                ?: return RecyclerView.NO_POSITION
        val forwardDirection: Boolean = isForwardFling(layoutManager, velocityX, velocityY)

        // A child that is exactly in the center is eligible for both before and after

        // A child that is exactly in the center is eligible for both before and after
        var closestChildHead: View? = null
        var distanceHead: Int? = null
        var closestChildEnd: View? = null
        var distanceEnd: Int? = null

        // Find the first view before the center, and the first view after the center
        val childCount = layoutManager.childCount
        for (i in 0 until childCount) {
            val child = layoutManager.getChildAt(i) ?: continue
            val childLp = child.layoutParams
                    ?.let { if (it is ScheduleCalendarLayoutManager.LayoutParams) it else null }
                    ?: continue
            if (!childLp.isDateLabel) {
                continue
            }
            val date = childLp.start ?: continue
            if (isHeadDate(date)) {
                val distance = distanceFromHead(layoutManager, child, orientationHelper)
                if (forwardDirection) {
                    if (distance > 0 && (distanceHead == null || distance < distanceHead)) {
                        closestChildHead = child
                        distanceHead = distance
                    }
                } else {
                    if (distance < 0 && (distanceHead == null || distance > distanceHead)) {
                        closestChildHead = child
                        distanceHead = distance
                    }
                }
            }
            if (isEndDate(date)) {
                val distance = distanceFromEnd(layoutManager, child, orientationHelper)
                if (forwardDirection) {
                    if (distance > 0 && (distanceEnd == null || distance < distanceEnd)) {
                        closestChildEnd = child
                        distanceEnd = distance
                    }
                } else {
                    if (distance < 0 && (distanceEnd == null || distance > distanceEnd)) {
                        closestChildEnd = child
                        distanceEnd = distance
                    }
                }
            }
        }

        // Return the position of the first child from the center, in the direction of the fling

        // Return the position of the first child from the center, in the direction of the fling
        if (closestChildHead != null) {
            return layoutManager.getPosition(closestChildHead)
        } else if (closestChildEnd != null) {
            return layoutManager.getPosition(closestChildEnd)
        }

        return RecyclerView.NO_POSITION
    }

    private fun getDate(v: View?): String {
        return v?.layoutParams?.let {
            if (it is ScheduleCalendarLayoutManager.LayoutParams) it.start?.toCalendar()?.let {
                "${it.get(Calendar.MONTH)}/${it.get(Calendar.DAY_OF_MONTH)}"
            } else null
        } ?: ""
    }

    private fun distanceFromHead(layoutManager: RecyclerView.LayoutManager, targetView: View, helper: OrientationHelper): Int {
        val lp = targetView.layoutParams?.let { if (it is ScheduleCalendarLayoutManager.LayoutParams) it else null }
        val childHead = helper.getDecoratedStart(targetView)
        return childHead - helper.startAfterPadding - (lp?.timeScaleWidth ?: 0)
    }

    private fun distanceFromEnd(layoutManager: RecyclerView.LayoutManager, targetView: View, helper: OrientationHelper): Int {
        val childEnd = helper.getDecoratedStart(targetView) + helper.getDecoratedMeasurement(targetView)
        return childEnd - helper.endAfterPadding
    }

    /**
     * Return the child view that is currently closest to the center of this parent.
     *
     * @param layoutManager The [RecyclerView.LayoutManager] associated with the attached
     * [RecyclerView].
     * @param helper The relevant [OrientationHelper] for the attached [RecyclerView].
     *
     * @return the child view that is currently closest to the center of this parent.
     */
    private fun findAnchorView(layoutManager: RecyclerView.LayoutManager, helper: OrientationHelper): View? {
        val childCount = layoutManager.childCount
        if (childCount == 0) {
            return null
        }
        // Find the anchor view with the shortest distance from the head or end position.
        var absDistance = Integer.MAX_VALUE
        var closestChild: View? = null
        val orientationHelper = getOrientationHelper(layoutManager) ?: return null
        for (i in 0 until childCount) {
            val child = layoutManager.getChildAt(i)
            val childLp = child?.layoutParams?.let { if (it is ScheduleCalendarLayoutManager.LayoutParams) it else null }
                    ?: continue
            if (!childLp.isDateLabel) {
                continue
            }
            val date = childLp.start ?: continue
            if (isHeadDate(date)) {
                val distance = abs(distanceFromHead(layoutManager, child, orientationHelper))
                if (distance < absDistance) {
                    closestChild = child
                    absDistance = distance
                }
            }
            if (isEndDate(date)) {
                val distance = abs(distanceFromEnd(layoutManager, child, orientationHelper))
                if (distance < absDistance) {
                    closestChild = child
                    absDistance = distance
                }
            }
        }
        return closestChild
    }

    private fun isForwardFling(layoutManager: RecyclerView.LayoutManager, velocityX: Int, velocityY: Int): Boolean {
        return velocityX > 0
    }

    private fun isHeadDate(date: Date): Boolean {
        return date.dayOfWeek() == startDayOfWeek
    }

    private fun isEndDate(date: Date): Boolean {
        return (startDayOfWeek - date.dayOfWeek()) % 7 == 1
    }

    private fun getOrientationHelper(layoutManager: RecyclerView.LayoutManager): OrientationHelper? {
        return if (layoutManager.canScrollHorizontally()) {
            getHorizontalHelper(layoutManager)
        } else {
            null
        }
    }

    private fun getHorizontalHelper(
            layoutManager: RecyclerView.LayoutManager): OrientationHelper {
        if (horizontalHelper?.layoutManager !== layoutManager) {
            horizontalHelper = null
        }
        return horizontalHelper ?: OrientationHelper.createHorizontalHelper(layoutManager)
    }
}