package co.thebabels.schedulecalendarview.view

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import co.thebabels.schedulecalendarview.R

/**
 * Simple view to display header surface.
 */
class CalendarHeaderView @JvmOverloads constructor(
        context: Context,
        attributeSet: AttributeSet? = null,
        defStyleAttr: Int = R.attr.calendarHeaderView,
) : FrameLayout(context, attributeSet, defStyleAttr) {

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
    }

    override fun offsetTopAndBottom(offset: Int) {
        // Do nothing
    }

    override fun offsetLeftAndRight(offset: Int) {
        // Do nothing
    }
}