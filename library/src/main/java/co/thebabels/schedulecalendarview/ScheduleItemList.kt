package co.thebabels.schedulecalendarview

import co.thebabels.schedulecalendarview.extention.isToday

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
    return this.filterNot { it.isDateLabel() }
}

/**
 * Returns the positions list that the item at given [position] overlaps with.
 */
fun List<ScheduleItem>.getOverlapPositions(position: Int, ignoreInclusion: Boolean = true): List<Int> {
    val positionsList = mutableListOf<Int>()
    val item = getOrNull(position) ?: return listOf()
    val start = item.start()
    // before
    for (i in position - 1 downTo 0) {
        val target = get(i)
        if (!target.start().isToday(start)) {
            break
        }
        if (item.isOverlap(target, ignoreInclusion)) {
            positionsList.add(0, i)
        }
    }
    // after
    for (i in position + 1 until size) {
        val target = get(i)
        if (!target.start().isToday(start)) {
            break
        }
        if (item.isOverlap(target, ignoreInclusion)) {
            positionsList.add(i)
        }
    }

    return positionsList
}
