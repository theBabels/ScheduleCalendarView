package co.thebabels.schedulecalendarview.view

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.Gravity
import androidx.appcompat.widget.AppCompatTextView

class DateLabelView @JvmOverloads constructor(
    context: Context,
    attributeSet: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : AppCompatTextView(context, attributeSet, defStyleAttr) {

    init {
        gravity = Gravity.CENTER
        setBackgroundColor(Color.YELLOW)
        elevation = 8f
    }

    override fun offsetTopAndBottom(offset: Int) {
        // Do nothing
    }
}