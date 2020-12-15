package co.thebabels.schedulecalendarview.view

import co.thebabels.schedulecalendarview.ScheduleCalendarAdapter
import co.thebabels.schedulecalendarview.ScheduleItem

/**
 * [ScheduleCalendarAdapter.ViewHolder] for [CurrentTimeView].
 */
class CurrentTimeViewHolder(itemView: CurrentTimeView) : ScheduleCalendarAdapter.ViewHolder(itemView) {
    override fun bind(item: ScheduleItem) {
        itemView.invalidate()
    }
}