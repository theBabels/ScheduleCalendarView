package co.thebabels.schedulecalendarview.extention

import android.util.Log
import java.util.*

private const val SECOND = 1000
private const val MINUTE = 60 * SECOND
private const val HOUR = 60 * MINUTE

fun Date.toCalendar(): Calendar {
    val d = this
    return Calendar.getInstance().apply { time = d }
}

fun Date.dayOfYear(): Int {
    return toCalendar().get(Calendar.DAY_OF_YEAR)
}

fun Date.dateDiff(target: Date): Int {
    val thisCal = toCalendar().apply { clearToMidnight() }
    val targetCal = target.toCalendar().apply { clearToMidnight() }
    return ((thisCal.time.time - targetCal.time.time) / (24 * HOUR)).toInt()
}

fun Date.hourDiff(target: Date): Float {
    val thisCal = toCalendar()
    val targetCal = target.toCalendar()
    return ((thisCal.time.time - targetCal.time.time).toFloat() / HOUR).apply {
        Log.d("TAG", "hourDiff='${this}'")
    }
}

fun Date.hourOfDay(): Float {
    return toCalendar().let { cal ->
        cal.get(Calendar.HOUR_OF_DAY) + (cal.get(Calendar.MINUTE).toFloat() / 60)
    }
}