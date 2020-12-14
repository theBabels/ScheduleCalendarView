package co.thebabels.schedulecalendarview

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.util.Log
import androidx.recyclerview.widget.RecyclerView

/**
 * [RecyclerView] working with [ScheduleCalendarLayoutManager].
 */
class ScheduleCalendarRecyclerView @JvmOverloads constructor(
        context: Context,
        attributeSet: AttributeSet? = null,
        defStyleAttr: Int = 0,
) : RecyclerView(context, attributeSet, defStyleAttr) {


    companion object {
        private const val TAG = "SCRecyclerView"
    }

    private var scrolledY = 0
    private var scrolledX = 0

    private var paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var columns = 7
    private var rowHeight = 0f
    private var dateLabelHeight = 0
    private var timeScaleWidth = 0

    init {
        overScrollMode
        // set up attrs
        context.theme.obtainStyledAttributes(
                attributeSet,
                R.styleable.ScheduleCalendarRecyclerView,
                defStyleAttr,
                R.style.ScheduleCalendarViewWidget_RecyclerView,
        ).apply {
            try {
                rowHeight = getDimension(R.styleable.ScheduleCalendarRecyclerView_rowHeight, 0f)
                // colors
                paint.color = getColor(R.styleable.ScheduleCalendarRecyclerView_gridLineColor, 0)
                dateLabelHeight = getDimensionPixelSize(R.styleable.ScheduleCalendarRecyclerView_dateLabelHeight, 0)
                timeScaleWidth = getDimensionPixelSize(R.styleable.ScheduleCalendarRecyclerView_timeScaleWidth, 0)
            } finally {
                recycle()
            }
        }

    }

    override fun setLayoutManager(layout: LayoutManager?) {
        super.setLayoutManager(layout)
        if (layout is ScheduleCalendarLayoutManager) {
            layout.rowHeight = this.rowHeight
            layout.dateLabelHeight = this.dateLabelHeight
            layout.timeScaleWidth = this.timeScaleWidth
        }
    }

    override fun onDraw(c: Canvas?) {
        super.onDraw(c)
        Log.v(TAG, "onDraw='${scrolledY}', '${scrollX}'")

        val offsetY = -scrolledY + dateLabelHeight
        for (i in 0 until ROWS_COUNT) {
            val y = offsetY + rowHeight * i
            c?.drawLine(0f, y, width.toFloat(), y, paint)
        }

        val rowWidth = (width - timeScaleWidth) / columns
        val offsetX = this.timeScaleWidth - (scrolledX % rowWidth)
        for (i in 0 until columns + 1) {
            val x = offsetX + rowWidth * i.toFloat()
            c?.drawLine(x, 0f, x, height.toFloat(), paint)
        }
    }

    override fun onScrolled(dx: Int, dy: Int) {
        super.onScrolled(dx, dy)
        Log.v(TAG, "onScrolled: dx='${dx}', dy='${dy}', scrolledX='${scrolledX}, 'scrolledY='${scrolledY}'")
        scrolledY += dy
        scrolledX += dx
    }
}