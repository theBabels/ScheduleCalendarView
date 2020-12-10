package co.thebabels.schedulecalendarview

fun List<ScheduleItem>.sort(): List<ScheduleItem> {
    return this.sortedWith { o1, o2 ->
        o1.compareTo(o2)
    }
}
