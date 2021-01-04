package co.thebabels.schedulecalendarview

import java.util.*

/**
 * Default implementation of [ScheduleCalendarLayoutManager.DateLookUp].
 */
class DefaultDateLookUp(private val adapter: ScheduleCalendarAdapter) :
        ScheduleCalendarLayoutManager.DateLookUp {

    override fun lookUpStart(position: Int): Date? {
        return adapter.getItem(position)?.start()
    }

    override fun lookUpEnd(position: Int): Date? {
        return adapter.getItem(position)?.end()
    }

    override fun lookUpOverlap(position: Int): ScheduleCalendarLayoutManager.OverlapInfo {
        val overlapPositions = adapter.getOverlapPositions(position).filter { !isDateLabel(it) && !isCurrentTime(it) }
        val beforePositions = overlapPositions.filter { it < position }
        return ScheduleCalendarLayoutManager.OverlapInfo(
                beforePositions = beforePositions,
                headPosition = beforePositions.lastOrNull { lookUpOverlap(it).headPosition == null },
                // TODO count max duplication count
                maxDuplicationCount = overlapPositions.size + 1
        )
    }

    override fun isDateLabel(position: Int): Boolean {
        return adapter.getItem(position)?.let {
            it is DateScheduleItem
        } ?: false
    }

    override fun isCurrentTime(position: Int): Boolean {
        return adapter.getItem(position)?.let {
            it is CurrentTimeScheduleItem
        } ?: false
    }

    override fun isStartSplit(position: Int): Boolean {
        return adapter.getItem(position)?.isStartSplit() == true
    }

    override fun isEndSplit(position: Int): Boolean {
        return adapter.getItem(position)?.isEndSplit() == true
    }
}