package co.thebabels.schedulecalendarview

import java.util.*

/**
 * [ScheduleItem] of a date label.
 */
data class DateScheduleItem(private val cal: Calendar) : ScheduleItem {

    companion object {

        /**
         * Returns a new [DateScheduleItem] from given [date].
         */
        fun fromDate(date: Date): DateScheduleItem {
            val cal = Calendar.getInstance().apply { time = date }
            return DateScheduleItem(cal)
        }

        /**
         * Returns a new [DateScheduleItem] of the first day of the week on a given [date].
         *
         * @param date the base date. if null, the current time is used.
         */
        fun firstDayOfWeek(date: Date? = null): DateScheduleItem {
            val cal = Calendar.getInstance()
            date?.let { cal.time = it }
            cal.add(Calendar.DAY_OF_MONTH, Calendar.MONDAY - cal.get(Calendar.DAY_OF_WEEK));
            return DateScheduleItem(cal)
        }
    }

    fun addDay(days: Int): DateScheduleItem {
        val c = Calendar.getInstance().apply { time = Date(cal.time.time) }
        c.add(Calendar.DATE, days)
        return DateScheduleItem(c)
    }

    fun nextDay(): DateScheduleItem {
        return addDay(1)
    }

    fun nextDays(size: Int, includeThis: Boolean = false): List<DateScheduleItem> {
        var initialDate = if (includeThis) {
            this
        } else {
            this.nextDay()
        }
        return List(size) { i ->
            initialDate.addDay(i)
        }
    }

    override fun start(): Date {
        return cal.time
    }

    override fun end(): Date {
        return cal.time
    }

    fun dateString(): String {
        return "${cal.get(Calendar.DATE)}"
    }

    fun dayOfWeekString(locale: Locale = Locale.getDefault()): String {
        return cal.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.SHORT, locale).orEmpty()
    }
}