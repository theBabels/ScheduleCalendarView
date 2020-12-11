package co.thebabels.schedulecalendarview.view

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import co.thebabels.schedulecalendarview.R
import co.thebabels.schedulecalendarview.ScheduleCalendarLayoutManager

/**
 * View displaying the time scale.
 * This view is also responsible for keeping track of the vertical scroll position in the calendar.
 */
class TimeScaleView @JvmOverloads constructor(
        context: Context,
        attributeSet: AttributeSet? = null,
        defStyleAttr: Int = R.attr.timeScaleView,
) : View(context, attributeSet, defStyleAttr) {

    companion object {
        private const val TAG = "TimeScaleView"
        val timeScaleRows = listOf<String>(
                "1:00",
                "2:00",
                "3:00",
                "4:00",
                "5:00",
                "6:00",
                "7:00",
                "8:00",
                "9:00",
                "10:00",
                "11:00",
                "12:00",
                "13:00",
                "14:00",
                "15:00",
                "16:00",
                "17:00",
                "18:00",
                "19:00",
                "20:00",
                "21:00",
                "22:00",
                "23:00",
        )
    }

    private var paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.RIGHT }
    private val textBounds = Rect(0, 0, 0, 0)
    private var rowHeight = 0f
    private var bgColor = 0

    init {
        elevation = 4f

        // common attributes
        context.theme.obtainStyledAttributes(
                attributeSet,
                R.styleable.ScheduleCalendarViewBase,
                defStyleAttr,
                R.style.ScheduleCalendarViewWidget_TimeScale,
        ).apply {
            try {
                // colors
                bgColor = getColor(R.styleable.ScheduleCalendarViewBase_backgroundColor, 0)
                paint.color = getColor(R.styleable.ScheduleCalendarViewBase_textColor, 0)
                // text size
                paint.textSize = getDimension(R.styleable.ScheduleCalendarViewBase_textSize, 0f)
            } finally {
                recycle()
            }
        }

        // init text bounds
        timeScaleRows.last().let { s ->
            paint.getTextBounds(s, 0, s.length, textBounds)
        }
    }


    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        rowHeight = layoutParams?.let {
            if (it is ScheduleCalendarLayoutManager.LayoutParams) it.rowHeight else 0f
        } ?: 0f
        setMeasuredDimension(
                getDefaultSize(0, widthMeasureSpec),
                rowHeight.toInt() * (timeScaleRows.size + 1)
        )
    }

    override fun onDraw(canvas: Canvas?) {
        canvas?.drawColor(bgColor)
        val x = width.toFloat() - ((textBounds.right - textBounds.left) / 3)
        timeScaleRows.forEachIndexed { index, s ->
            val y = rowHeight * (index + 1)
            canvas?.drawText(
                    s,
                    x,
                    y + ((textBounds.bottom - textBounds.top) / 2),
                    paint
            )
        }
    }

    override fun offsetLeftAndRight(offset: Int) {
        // Do nothing
    }
}