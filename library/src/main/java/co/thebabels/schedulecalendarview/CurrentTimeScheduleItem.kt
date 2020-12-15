package co.thebabels.schedulecalendarview

import java.util.*

data class CurrentTimeScheduleItem(private val date: Date) : ScheduleItem {

    companion object {
        fun now(): CurrentTimeScheduleItem {
            return CurrentTimeScheduleItem(Date())
        }
    }

    override fun start(): Date {
        return this.date
    }

    override fun end(): Date {
        return this.date
    }
}