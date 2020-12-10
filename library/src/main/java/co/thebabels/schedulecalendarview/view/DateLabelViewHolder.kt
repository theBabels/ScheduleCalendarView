package co.thebabels.schedulecalendarview.view

import co.thebabels.schedulecalendarview.DateScheduleItem
import co.thebabels.schedulecalendarview.ScheduleCalendarAdapter
import co.thebabels.schedulecalendarview.ScheduleItem

/**
 * [ScheduleCalendarAdapter.ViewHolder] for [DateLabelView].
 */
class DateLabelViewHolder(itemView: DateLabelView) : ScheduleCalendarAdapter.ViewHolder(itemView) {
    override fun bind(item: ScheduleItem) {
        if (itemView is DateLabelView) {
            if (item is DateScheduleItem) {
                itemView.bindDate(item)
            }
        }
    }
}