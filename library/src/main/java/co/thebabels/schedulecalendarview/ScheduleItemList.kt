package co.thebabels.schedulecalendarview

fun List<ScheduleItem>.sort(): List<ScheduleItem> {
    return this.sortedWith { o1, o2 ->
        o1.start().compareTo(o2.start()).let { startCompare ->
            when (startCompare) {
                0 -> o1.end().compareTo(o2.end())
                else -> startCompare
            }
        }
    }
}
