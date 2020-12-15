package co.thebabels.schedulecalendarview.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import co.thebabels.schedulecalendarview.R
import kotlin.math.max

/**
 * View to display current time in schedule calendar.
 */
class CurrentTimeView @JvmOverloads constructor(
        context: Context,
        attributeSet: AttributeSet? = null,
        defStyleAttr: Int = R.attr.currentTimeView,
) : View(context, attributeSet, defStyleAttr) {

    private var paint = Paint(Paint.ANTI_ALIAS_FLAG)

    init {
        elevation = 1f

        // set up attrs
        context.theme.obtainStyledAttributes(
                attributeSet,
                R.styleable.CurrentTimeView,
                defStyleAttr,
                R.style.ScheduleCalendarViewWidget_CurrentTimeView,
        ).apply {
            try {
                // colors
                paint.color = getColor(R.styleable.CurrentTimeView_lineColor, 0)
            } finally {
                recycle()
            }
        }
    }

    override fun onDraw(canvas: Canvas?) {
        canvas?.drawColor(Color.TRANSPARENT)
        val circleSize = height / 2f - 1f
        val lineHeight = max(height / 4f, 1f)
        canvas?.drawCircle(circleSize, height / 2f, circleSize, paint)
        canvas?.drawRect(circleSize, height / 2f - lineHeight / 2f, width.toFloat(), height / 2f + lineHeight / 2f, paint)
    }
}