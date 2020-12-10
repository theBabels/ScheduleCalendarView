package co.thebabels.schedulecalendarview.view

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import android.view.View
import androidx.core.content.ContextCompat
import co.thebabels.schedulecalendarview.DateScheduleItem
import co.thebabels.schedulecalendarview.R

/**
 * Date label view to display in calendar header.
 */
class DateLabelView @JvmOverloads constructor(
    context: Context,
    attributeSet: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attributeSet, defStyleAttr) {

    var date: DateScheduleItem? = null
    private var paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textBounds = Rect(0, 0, 0, 0)
    private var bgColor = Color.WHITE
    private var dateTextSize: Int = 0
    private var dateTextColor = ContextCompat.getColor(context, R.color.date_label_date_text)
    private var dateTypeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    private var dayOfWeekTextSize: Int = 0
    private var dayOfWeekTextColor =
        ContextCompat.getColor(context, R.color.date_label_day_of_week_text)
    private var dayOfWekTypeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)

    init {
        elevation = 8f
        paint.color = ContextCompat.getColor(context, R.color.grid_line)
        paint.textAlign = Paint.Align.CENTER
        dateTextSize = resources.getDimensionPixelSize(R.dimen.date_label_date_text_size)
        dayOfWeekTextSize =
            resources.getDimensionPixelSize(R.dimen.date_label_day_of_week_text_size)
    }

    /**
     * Bind the [DateScheduleItem] to be displayed.
     */
    fun bindDate(date: DateScheduleItem?) {
        this.date = date
        invalidate()
    }

    override fun onDraw(canvas: Canvas?) {
        canvas?.drawColor(bgColor)

        // write day of week
        val dayOfWeekText = date?.dayOfWeekString().orEmpty()
        paint.textSize = dayOfWeekTextSize.toFloat()
        paint.color = dayOfWeekTextColor
        paint.typeface = dayOfWekTypeface
        paint.getTextBounds(dayOfWeekText, 0, dayOfWeekText.length, textBounds)
        Log.d("HOGE", "${textBounds.bottom}, ${textBounds.top}")
        canvas?.drawText(
            dayOfWeekText,
            width / 2f,
            (textBounds.bottom - textBounds.top).toFloat() + paddingTop,
            paint
        )

        // write date
        val dateText = date?.dateString().orEmpty()
        paint.textSize = dateTextSize.toFloat()
        paint.color = dateTextColor
        paint.typeface = dateTypeface
        val offsetY = (textBounds.bottom - textBounds.top).toFloat() + paddingTop
        paint.getTextBounds(dateText, 0, dateText.length, textBounds)
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