package co.thebabels.schedulecalendarview

import java.util.*


interface ScheduleItem {
    fun start(): Date
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
