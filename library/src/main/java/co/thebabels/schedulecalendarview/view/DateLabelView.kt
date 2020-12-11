package co.thebabels.schedulecalendarview.view

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import android.view.View
import co.thebabels.schedulecalendarview.DateScheduleItem
import co.thebabels.schedulecalendarview.R
import co.thebabels.schedulecalendarview.extention.isToday

/**
 * Date label view to display in calendar header.
 */
class DateLabelView @JvmOverloads constructor(
    context: Context,
    attributeSet: AttributeSet? = null,
    defStyleAttr: Int = R.attr.dateLabelView,
) : View(context, attributeSet, defStyleAttr) {

    var date: DateScheduleItem? = null
    var isToday: Boolean = false
    private var paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER }
    private val textBounds = Rect(0, 0, 0, 0)
    private var bgColor = 0
    private var dateTextSize = 0f
    private var dateTextColor = 0
    private var dateTypeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    private var dayOfWeekTextSize = 0f
    private var dayOfWeekTextColor = 0
    private var dayOfWekTypeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    private var todayColor = 0
    private var onTodayColor = 0
    private var todayCirclePadding = 0f

    init {
        elevation = 8f

        // set up attrs
        context.theme.obtainStyledAttributes(
            attributeSet,
            R.styleable.DateLabelView,
            defStyleAttr,
            R.style.DateLabel,
        ).apply {
            try {
                // colors
                bgColor = getColor(R.styleable.DateLabelView_backgroundColor, 0)
                dateTextColor = getColor(R.styleable.DateLabelView_dateTextColor, 0)
                dayOfWeekTextColor = getColor(R.styleable.DateLabelView_dayOfWeekTextColor, 0)
                todayColor = getColor(R.styleable.DateLabelView_todayColor, 0)
                onTodayColor = getColor(R.styleable.DateLabelView_onTodayColor, 0)
                // text size
                dateTextSize = getDimension(R.styleable.DateLabelView_dateTextSize, 0f)
                dayOfWeekTextSize = getDimension(R.styleable.DateLabelView_dayOfWeekTextSize, 0f)
                // others
                todayCirclePadding = getDimension(R.styleable.DateLabelView_todayCirclePadding, 0f)
            } finally {
                recycle()
            }
        }
    }

    /**
     * Bind the [DateScheduleItem] to be displayed.
     */
    fun bindDate(date: DateScheduleItem?) {
        this.date = date
        this.isToday = date?.start()?.isToday() ?: false
        invalidate()
    }

    override fun onDraw(canvas: Canvas?) {
        canvas?.drawColor(bgColor)

        // write day of week
        val dayOfWeekText = date?.dayOfWeekString().orEmpty()
        paint.textSize = dayOfWeekTextSize.toFloat()
        paint.color = if (isToday) todayColor else dayOfWeekTextColor
        paint.typeface = dayOfWekTypeface
        paint.getTextBounds(dayOfWeekText, 0, dayOfWeekText.length, textBounds)
        Log.d("HOGE", "${textBounds.bottom}, ${textBounds.top}")
        canvas?.drawText(
            dayOfWeekText,
            width / 2f,
            (textBounds.bottom - textBounds.top).toFloat() + paddingTop,
            paint
        )

        val offsetY = (textBounds.bottom - textBounds.top).toFloat() + paddingTop

        // write date
        val dateText = date?.dateString().orEmpty()
        paint.textSize = dateTextSize.toFloat()
        paint.typeface = dateTypeface
        paint.getTextBounds(dateText, 0, dateText.length, textBounds)
        // write today circle
        if (isToday) {
            paint.color = todayColor
            canvas?.drawCircle(
                width / 2f,
                offsetY + (height - offsetY) / 2f,
                (textBounds.bottom - textBounds.top).toFloat() + todayCirclePadding * 2,
                paint
            )
        }
        // write date text
        paint.color = if (isToday) onTodayColor else dateTextColor
        canvas?.drawText(
            dateText,
            width / 2f,
            offsetY + (height - offsetY) / 2f + (textBounds.bottom - textBounds.top) / 2,
            paint
        )
    }

    override fun offsetTopAndBottom(offset: Int) {
        // Do nothing
    }
}