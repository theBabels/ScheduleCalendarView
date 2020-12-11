package co.thebabels.schedulecalendarview

import java.util.*

/**
 * The interface that the items displayed in the calendar should implement.
 */
interface ScheduleItem {
    /**
     * Returns the start date.
     */
    fun start(): Date

    /**
     * Returns the end date.
     */
    fun end(): Date

    fun compareTo(target: ScheduleItem): Int {
        return this.start().compareTo(target.start()).let { startCompare ->
            when (startCompare) {
                0 -> this.end().compareTo(target.end())
                else -> startCompare
            }
        }
    }
}
