package co.thebabels.schedulecalendarview

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat


class TimeScaleView @JvmOverloads constructor(
    context: Context,
    attributeSet: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attributeSet, defStyleAttr) {

    companion object {
        private const val TAG = "TimeScaleView"
        val timeScaleRows = listOf<String>(
            "0:00",
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

    private var paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var minWidth = 400
    private var rowHeight: Float = 200f
    val textBounds = Rect(0, 0, 0, 0)

    init {
        paint.color = ContextCompat.getColor(context, R.color.grid_line)
        paint.textSize = 36f
        paint.textAlign = Paint.Align.RIGHT
        timeScaleRows.last().let { s ->
            paint.getTextBounds(s, 0, s.length, textBounds)
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        setMeasuredDimension(
            getDefaultSize(minWidth, widthMeasureSpec),
            rowHeight.toInt() * (timeScaleRows.size + 1)
        )
    }

    override fun onDraw(canvas: Canvas?) {
        // TODO enable set background color
        canvas?.drawARGB(255, 255, 255, 255)
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
}