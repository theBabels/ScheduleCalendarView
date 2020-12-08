package co.thebabels.schedulecalendarview

import java.util.*


interface ScheduleItem {
    fun start(): Date
    fun end(): Date
}
