package co.thebabels.schedulecalendarview.view

import android.widget.TextView
import co.thebabels.schedulecalendarview.DateScheduleItem
import co.thebabels.schedulecalendarview.ScheduleCalendarAdapter
import co.thebabels.schedulecalendarview.ScheduleItem

class DateLabelViewHolder(itemView: DateLabelView) : ScheduleCalendarAdapter.ViewHolder(itemView) {
    override fun bind(item: ScheduleItem) {
        if (itemView is TextView) {
            if (item is DateScheduleItem) {
                itemView.text = item.dateString()
            }
        }
    }
}