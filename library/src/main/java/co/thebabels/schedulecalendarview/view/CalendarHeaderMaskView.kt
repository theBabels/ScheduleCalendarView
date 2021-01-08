package co.thebabels.schedulecalendarview.view

import android.content.Context
import android.util.AttributeSet
import android.view.View
import co.thebabels.schedulecalendarview.R

class CalendarHeaderMaskView @JvmOverloads constructor(
        context: Context,
        attributeSet: AttributeSet? = null,
        defStyleAttr: Int = R.attr.calendarHeaderView, // read from calendar header view
) : View(context, attributeSet, defStyleAttr) {

    init {
        // set up attrs
        context.theme.obtainStyledAttributes(
                attributeSet,
                R.styleable.ScheduleCalendarViewBase,
                defStyleAttr,
                R.style.ScheduleCalendarViewWidget_CalendarHeaderView,
        ).apply {
            try {
                val bgColor = getColor(R.styleable.ScheduleCalendarViewBase_backgroundColor, Integer.MIN_VALUE)
                if (bgColor != Integer.MIN_VALUE) {
                    setBackgroundColor(bgColor)
                }
            } finally {
                recycle()
            }
        }
        outlineProvider = null
    }

    override fun offsetTopAndBottom(offset: Int) {
        // Do nothing
    }

    override fun offsetLeftAndRight(offset: Int) {
        // Do nothing
    }
}