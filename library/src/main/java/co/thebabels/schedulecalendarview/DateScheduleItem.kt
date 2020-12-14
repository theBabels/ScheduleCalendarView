package co.thebabels.schedulecalendarview

import co.thebabels.schedulecalendarview.extention.clearToMidnight
import co.thebabels.schedulecalendarview.extention.dateDiff
import co.thebabels.schedulecalendarview.extention.toCalendar
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
            val cal = date.toCalendar()
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

    init {
        cal.clearToMidnight()
    }

    /**
     * Returns a [DateScheduleItem] after specified date.
     */
    fun addDay(days: Int): DateScheduleItem {
        val c = Calendar.getInstance().apply { time = Date(cal.time.time) }
        c.add(Calendar.DATE, days)
        return DateScheduleItem(c)
    }

    /**
     * Returns a [DateScheduleItem] that is next day of this schedule item.
     */
    fun nextDay(): DateScheduleItem {
        return addDay(1)
    }

    /**
     * Returns a list of [DateScheduleItem] following this schedule.
     *
     * @param size the size of list.
     * @param includeThis if true, the first item of the list is this [DateScheduleItem], if false, it is the next day of this.
     */
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

    /**
     * Returns a list of [DateScheduleItem] from this schedule to [endDate].
     */
    fun nextDays(endDate: Date, includeThis: Boolean = false): List<DateScheduleItem> {
        val size = endDate.dateDiff(this.start())
        if (size <= 0) return listOf()
        return nextDays(size, includeThis)
    }

    /**
     * Returns a [DateScheduleItem] that is the previous day of this schedule item.
     */
    fun previousDay(): DateScheduleItem {
        return addDay(-1)
    }

    /**
     * Returns a list of [DateScheduleItem] previous of this schedule.
     *
     * @param size the size of the returned list.
     */
    fun previousDays(size: Int, includeThis: Boolean = false): List<DateScheduleItem> {
        var initialDate = if (includeThis) {
            this
        } else {
            this.previousDay()
        }
        return List(size) { i ->
            initialDate.addDay(-size + 1 + i)
        }
    }

    /**
     * Returns a list of [DateScheduleItem] from [startDate] to this schedule.
     */
    fun previousDays(startDate: Date, includeThis: Boolean = false): List<DateScheduleItem> {
        val size = this.start().dateDiff(startDate)
        if (size <= 0) return listOf()
        return previousDays(size, includeThis)
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