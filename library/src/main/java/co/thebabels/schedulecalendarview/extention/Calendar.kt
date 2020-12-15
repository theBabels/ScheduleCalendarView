package co.thebabels.schedulecalendarview.extention

import java.util.*

/**
 * Set time to "00:00", i.e. clear hour, minute, second and milli second fields.
 */
fun Calendar.clearToMidnight() {
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}