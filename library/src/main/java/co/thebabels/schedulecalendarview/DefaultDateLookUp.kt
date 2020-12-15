package co.thebabels.schedulecalendarview

import java.util.*

/**
 * Default implementation of [ScheduleCalendarLayoutManager.DateLookUp].
 */
class DefaultDateLookUp(private val adapter: ScheduleCalendarAdapter) :
        ScheduleCalendarLayoutManager.DateLookUp {

    override fun lookUpStart(position: Int): Date? {
        return adapter.getItem(position)?.start()
    }

    override fun lookUpEnd(position: Int): Date? {
        return adapter.getItem(position)?.end()
    }

    override fun isDateLabel(position: Int): Boolean {
        return adapter.getItem(position)?.let {
            it is DateScheduleItem
        } ?: false
    }

    override fun isCurrentTime(position: Int): Boolean {
        return adapter.getItem(position)?.let {
            it is CurrentTimeScheduleItem
        } ?: false
    }
}