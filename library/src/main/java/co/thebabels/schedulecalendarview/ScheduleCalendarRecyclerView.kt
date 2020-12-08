package co.thebabels.schedulecalendarview

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView


class ScheduleCalendarRecyclerView @JvmOverloads constructor(
    context: Context,
    attributeSet: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : RecyclerView(context, attributeSet, defStyleAttr) {


    companion object {
        private const val TAG = "SCRecyclerView"
        const val ROWS_COUNT = 24
    }

    private var scrolledY = 0

    private var paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var columns: Int = 7
    private var rowHeight: Float = 200f

    init {
        paint.color = ContextCompat.getColor(context, R.color.grid_line)
    }

    override fun setLayoutManager(layout: LayoutManager?) {
        super.setLayoutManager(layout)
        if (layout is ScheduleCalendarLayoutManager) {
            this.rowHeight = layout.rowHeight
        }
    }

    override fun onDraw(c: Canvas?) {
        super.onDraw(c)
        Log.println(Log.DEBUG, TAG, "onDraw='${scrolledY}', '${scrollX}'")
        val offsetY = -scrolledY
        for (i in 0 until ScheduleCalendarView.ROWS_COUNT) {
            val y = offsetY + rowHeight * i
            c?.drawLine(0f, y, width.toFloat(), y, paint)
        }

        val rowWidth = width / columns.toFloat()
        for (i in 0 until columns) {
            val x = rowWidth * i.toFloat()
            c?.drawLine(x, 0f, x, height.toFloat(), paint)
        }
    }

    override fun onScrolled(dx: Int, dy: Int) {
        super.onScrolled(dx, dy)
        Log.d(TAG, "onScrolled: ${dx}, ${dy}, ${scrollY}")
        scrolledY += dy
    }


}