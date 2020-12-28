package co.thebabels.schedulecalendarview

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
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

    private var paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var columns = 7
    private var rowHeight = 0f
    private var dateLabelHeight = 0
    private var timeScaleWidth = 0
    private var currentTimeHeight = 0
    private var itemRightPadding = 0

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
                currentTimeHeight = getDimensionPixelSize(R.styleable.ScheduleCalendarRecyclerView_currentTimeHeight, 0)
                itemRightPadding = getDimensionPixelSize(R.styleable.ScheduleCalendarRecyclerView_itemRightPadding, 0)
            } finally {
                recycle()
            }
        }

    }

    override fun setLayoutManager(layout: LayoutManager?) {
        super.setLayoutManager(layout)
        if (layout is ScheduleCalendarLayoutManager) {
            layout.daysCount = this.columns
            layout.rowHeight = this.rowHeight
            layout.dateLabelHeight = this.dateLabelHeight
            layout.timeScaleWidth = this.timeScaleWidth
            layout.currentTimeHeight = this.currentTimeHeight
            layout.itemRightPadding = this.itemRightPadding
        }
    }


    override fun onDraw(c: Canvas?) {
        super.onDraw(c)
        val lm = layoutManager?.let {
            if (it is ScheduleCalendarLayoutManager) it else null
        }

        val offsetY = (lm?.currentVerticalScroll() ?: 0) % rowHeight + dateLabelHeight
        for (i in 0 until ROWS_COUNT) {
            val y = offsetY + rowHeight * i
            c?.drawLine(0f, y, width.toFloat(), y, paint)
        }

        val rowWidth = (width - timeScaleWidth) / columns
        val offsetX = lm?.getFirstDateLabelX() ?: 0f
        for (i in 0 until columns + 5) {
            val x = offsetX + rowWidth * i.toFloat()
            if (x > width) {
                break
            }
            c?.drawLine(x, 0f, x, height.toFloat(), paint)
        }
    }

    fun columnWidth(): Float {
        return (width - timeScaleWidth) / columns.toFloat()
    }

    fun rowHeight(): Float {
        return rowHeight
    }
}