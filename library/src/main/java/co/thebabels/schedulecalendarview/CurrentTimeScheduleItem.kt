package co.thebabels.schedulecalendarview

import java.util.*

data class CurrentTimeScheduleItem(private val date: Date) : ScheduleItem {

    companion object {
        fun now(): CurrentTimeScheduleItem {
            return CurrentTimeScheduleItem(Date())
        }
    }

    override fun key(): String {
        return "CurrentTimeScheduleItem"
    }

    override fun start(): Date {
        return this.date
    }

    override fun end(): Date {
        return this.date
    }

    override fun getOrigin(): ScheduleItem? {
        return null
    }

    override fun setOrigin(origin: ScheduleItem?) {
        // Do nothing
    }

    override fun update(start: Date, end: Date): ScheduleItem {
        return copy(date = start)
    }
}