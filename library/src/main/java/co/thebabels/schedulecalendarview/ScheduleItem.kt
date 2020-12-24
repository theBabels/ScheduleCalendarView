package co.thebabels.schedulecalendarview

import co.thebabels.schedulecalendarview.extention.clearToMidnight
import co.thebabels.schedulecalendarview.extention.isToday
import co.thebabels.schedulecalendarview.extention.minuteDiff
import co.thebabels.schedulecalendarview.extention.toCalendar
import java.util.*

/**
 * The interface that the items displayed in the calendar should implement.
 */
interface ScheduleItem {

    /**
     * Returns a key of this item.
     */
    fun key(): String

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
     * Returns a original [ScheduleItem] that is set by [setOrigin]
     */
    fun getOrigin(): ScheduleItem?

    /**
     * Set a original [ScheduleItem]. This is called from [splitAtMidnight].
     */
    fun setOrigin(origin: ScheduleItem?)

    /**
     * Returns a new [ScheduleItem] that reflects the updates of [start] and [end] of this item in the original item.
     */
    fun reflectUpdateToOrigin(start: Date, end: Date): ScheduleItem {
        // FIXME Make it possible to explicitly specify whether it is a move or a change in the start/end position.
        val origin = getOrigin() ?: return update(start, end)
        return if (start == start()) {
            // end is changed
            if (isEndSplit()) {
                origin
            } else {
                origin.update(origin.start(), end)
            }
        } else if (end == end()) {
            // start is changed
            if (isStartSplit()) {
                origin
            } else {
                origin.update(start, origin.end())
            }
        } else {
            // move
            val minuteDiff = if (isStartSplit()) {
                end.minuteDiff(end())
            } else {
                start.minuteDiff(start())
            }
            origin.update(
                    origin.start().toCalendar().apply { add(Calendar.MINUTE, minuteDiff.toInt()) }.time,
                    origin.end().toCalendar().apply { add(Calendar.MINUTE, minuteDiff.toInt()) }.time,
            )
        }
    }

    /**
     * Return true if the [start] position is a split one.
     *
     * @see [splitAtMidnight]
     */
    fun isStartSplit(): Boolean {
        return getOrigin()?.let { origin -> start().after(origin.start()) } ?: false
    }

    /**
     * Return true if the [end] is a split one.
     *
     * @see [splitAtMidnight]
     */
    fun isEndSplit(): Boolean {
        return getOrigin()?.let { origin -> end().before(origin.end()) } ?: false
    }

    fun splitAtMidnight(): List<ScheduleItem> {
        if (start().isToday(end())) {
            return listOf(this)
        }

        val origin = this
        val list = mutableListOf<ScheduleItem>()
        val s = start().toCalendar()
        val e = end()

        while (true) {
            val start = s.time
            s.add(Calendar.DATE, 1)
            s.clearToMidnight()
            val endOfDate = s.time
            if (e.after(endOfDate)) {
                list.add(update(start, endOfDate).apply { setOrigin(origin) })
            } else {
                list.add(update(start, e).apply { setOrigin(origin) })
                break
            }
        }
        return list
    }

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
