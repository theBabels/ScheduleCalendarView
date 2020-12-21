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

    /**
     * Returns a new [ScheduleItem] that [start] and [end] is updated.
     */
    fun update(start: Date, end: Date): ScheduleItem

    /**
     * @return  the value <code>0</code> if the argument Date is equal to
     *          this Date; a value less than <code>0</code> if this Date
     *          is before the Date argument; and a value greater than
     *      <code>0</code> if this Date is after the Date argument.
     */
    fun compareTo(target: ScheduleItem): Int {
        return this.start().compareTo(target.start()).let { startCompare ->
            when (startCompare) {
                0 -> this.end().compareTo(target.end())
                else -> startCompare
            }
        }
    }
}
