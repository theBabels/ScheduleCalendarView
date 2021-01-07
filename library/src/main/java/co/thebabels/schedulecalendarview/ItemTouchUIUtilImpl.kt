package co.thebabels.schedulecalendarview

import android.graphics.Canvas
import android.os.Build
import android.util.Log
import android.view.View
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.ItemTouchUIUtil
import androidx.recyclerview.widget.RecyclerView
import co.thebabels.schedulecalendarview.view.DateLabelView

internal class ItemTouchUIUtilImpl : ItemTouchUIUtil {
    override fun onDraw(c: Canvas, recyclerView: RecyclerView, view: View, dX: Float, dY: Float,
                        actionState: Int, isCurrentlyActive: Boolean) {
        if (Build.VERSION.SDK_INT >= 21) {
            if (isCurrentlyActive) {
                var originalElevation = view.getTag(R.id.item_touch_helper_previous_elevation)
                if (originalElevation == null) {
                    originalElevation = ViewCompat.getElevation(view)
                    val newElevation =
                            findHeaderElevation(view)?.let { it - 1f }
                                    ?: findMaxElevation(recyclerView, view) + 1f
                    ViewCompat.setElevation(view, newElevation)
                    view.setTag(R.id.item_touch_helper_previous_elevation, originalElevation)
                }
            }
        }
        val lp = view.layoutParams as ScheduleCalendarLayoutManager.LayoutParams
        when (actionState) {
            ScheduleCalendarItemTouchHelper.ACTION_STATE_DRAG -> {
                view.translationX = dX
                if (!lp.isStartSplit) {
                    view.translationY = dY
                }
            }
            ScheduleCalendarItemTouchHelper.ACTION_STATE_DRAG_START -> {
                // Top position changes, so position correction is needed to make it look good.
                // If the view is dragged downward beyond the height of the view, it will be fixed at the bottom(=schedule end) position,
                // so there is no need for translation.
                if (dY < view.height) {
                    view.translationY = dY
                }
            }
            ScheduleCalendarItemTouchHelper.ACTION_STATE_DRAG_END -> {
                // Do nothing
            }
        }
    }

    override fun onDrawOver(c: Canvas, recyclerView: RecyclerView, view: View, dX: Float, dY: Float,
                            actionState: Int, isCurrentlyActive: Boolean) {
    }

    override fun clearView(view: View) {
        Log.v("ItemTouchUIUtilImpl", "clearView: '${view}'")
        if (Build.VERSION.SDK_INT >= 21) {
            val tag = view.getTag(R.id.item_touch_helper_previous_elevation)
            if (tag is Float) {
                ViewCompat.setElevation(view, tag)
            }
            view.setTag(R.id.item_touch_helper_previous_elevation, null)
        }
        view.translationX = 0f
        view.translationY = 0f
    }

    override fun onSelected(view: View) {}

    companion object {
        val INSTANCE: ItemTouchUIUtil = ItemTouchUIUtilImpl()

        private fun findHeaderElevation(itemView: View): Float? {
            val lp = itemView.layoutParams
            if (lp is ScheduleCalendarLayoutManager.LayoutParams) {
                return lp.headerElevation.toFloat()
            }
            return null
        }

        private fun findMaxElevation(recyclerView: RecyclerView, itemView: View): Float {
            val childCount = recyclerView.childCount
            var max = 0f
            for (i in 0 until childCount) {
                val child = recyclerView.getChildAt(i)
                if (child === itemView) {
                    continue
                }
                val elevation = ViewCompat.getElevation(child)
                if (elevation > max) {
                    max = elevation
                }
            }
            return max
        }
    }
}