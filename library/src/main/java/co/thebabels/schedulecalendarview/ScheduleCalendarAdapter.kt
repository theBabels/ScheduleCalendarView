package co.thebabels.schedulecalendarview

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import java.lang.IllegalArgumentException


abstract class ScheduleCalendarAdapter<I : ScheduleItem>() :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        const val ViewTypeTimeScale = 100
        const val ViewTypeSchedule = 101
    }

    private val items: MutableList<I> = mutableListOf()

    abstract fun createScheduleViewHolder(parent: ViewGroup): ViewHolder<I>

    fun addItems(vararg items: I) {
        items.forEach {
            this.items.add(it)
        }
        this.items.sort()
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (position) {
            0 -> Unit
            else -> {
//                (holder as ViewHolder<I>).bind(items[position])
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ViewTypeTimeScale -> TimeScaleViewHolder(TimeScaleView(parent.context))
            ViewTypeSchedule -> createScheduleViewHolder(parent)
            else -> throw IllegalArgumentException("unknown view type: ${viewType}")
        }
    }

    override fun getItemCount(): Int {
        // item + timeScaleView
        return items.size + 1
    }

    override fun getItemViewType(position: Int): Int {
        return when (position) {
            0 -> ViewTypeTimeScale
            else -> ViewTypeSchedule
        }
    }

    abstract class ViewHolder<I : ScheduleItem>(itemView: View) :
        RecyclerView.ViewHolder(itemView) {

        abstract fun bind(item: I)
    }

    class TimeScaleViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {}
}