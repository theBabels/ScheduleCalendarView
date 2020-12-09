package co.thebabels.schedulecalendarview

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import co.thebabels.schedulecalendarview.view.DateLabelView
import co.thebabels.schedulecalendarview.view.DateLabelViewHolder
import co.thebabels.schedulecalendarview.view.TimeScaleView
import co.thebabels.schedulecalendarview.view.TimeScaleViewHolder
import java.lang.IllegalArgumentException


abstract class ScheduleCalendarAdapter() :
    RecyclerView.Adapter<ScheduleCalendarAdapter.ViewHolder>() {

    companion object {
        const val ViewTypeTimeScale = 100
        const val ViewTypeDateLabel = 101
        const val ViewTypeSchedule = 102
    }

    private val items: MutableList<ScheduleItem> = mutableListOf()

    abstract fun createScheduleViewHolder(parent: ViewGroup): ViewHolder

    /**
     * Returns a [ScheduleItem] at given [position].
     *
     * @param position adapter position
     */
    fun getItem(position: Int): ScheduleItem? {
        return items.getOrNull(position - 1)
    }

    fun addItems(vararg items: ScheduleItem) {
        this.items.addAll(items.toList().sort())
        // TODO specific position
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {
        return when (viewType) {
            ViewTypeTimeScale -> TimeScaleViewHolder(TimeScaleView(parent.context))
            ViewTypeSchedule -> createScheduleViewHolder(parent)
            ViewTypeDateLabel -> DateLabelViewHolder(DateLabelView(parent.context))
            else -> throw IllegalArgumentException("unknown view type: ${viewType}")
        }
    }


    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        when (position) {
            0 -> Unit
            else -> {
                holder.bind(items[position - 1])
            }
        }
    }

    override fun getItemCount(): Int {
        // item + timeScaleView
        return items.size + 1
    }

    override fun getItemViewType(position: Int): Int {
        return when (position) {
            0 -> ViewTypeTimeScale
            else -> {
                if (items.getOrNull(position - 1) is DateScheduleItem) {
                    ViewTypeDateLabel
                } else {
                    ViewTypeSchedule
                }
            }
        }
    }

    abstract class ViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView) {

        abstract fun bind(item: ScheduleItem)
    }
}