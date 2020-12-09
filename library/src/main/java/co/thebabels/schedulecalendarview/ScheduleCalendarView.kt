package co.thebabels.schedulecalendarview

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.content.ContextCompat

class ScheduleCalendarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ViewGroup(context, attrs, defStyleAttr) {

    companion object {
        const val ROWS_COUNT = 25
    }

    private var paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var columns: Int = 7
    private var rowHeight: Float = 100f

    init {
        paint.color = ContextCompat.getColor(context, R.color.grid_line)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {

    }

    override fun onDraw(canvas: Canvas?) {

        super.onDraw(canvas)
        for (i in 0 until ROWS_COUNT) {
            val y = rowHeight * i
            canvas?.drawLine(0f, y, width.toFloat(), y, paint)
        }

        val rowWidth = width / columns.toFloat()
        for (i in 0 until columns) {
            val x = rowWidth * i.toFloat()
            canvas?.drawLine(x, 0f, x, height.toFloat(), paint)
        }
    }
}