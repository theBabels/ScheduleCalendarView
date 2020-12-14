package co.thebabels.schedulecalendarview

/**
 * Returns the sorted schedule item list.
 */
fun List<ScheduleItem>.sort(): List<ScheduleItem> {
    return this.sortedWith { o1, o2 ->
        o1.compareTo(o2)
    }
}

/**
 *　Return a list containing only [DateScheduleItem].
 */
fun List<ScheduleItem>.filterDateScheduleItems(): List<DateScheduleItem> {
    return this.filterIsInstance<DateScheduleItem>()
}

/**
 * Return a list excluding [DateScheduleItem].
 */
fun List<ScheduleItem>.filterNotDateScheduleItems(): List<ScheduleItem> {
    return this.filterNot { it is DateScheduleItem }
}


