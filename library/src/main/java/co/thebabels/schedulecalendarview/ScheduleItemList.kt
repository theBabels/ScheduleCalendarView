package co.thebabels.schedulecalendarview


fun List<ScheduleItem>.sort(): List<ScheduleItem> {
    return this.sortedBy { it.start() }
}
