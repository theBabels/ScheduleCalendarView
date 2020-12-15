package co.thebabels.schedulecalendarview

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import co.thebabels.schedulecalendarview.extention.dateDiff
import co.thebabels.schedulecalendarview.view.*
import java.lang.IllegalArgumentException
import java.util.*


abstract class ScheduleCalendarAdapter() :
        RecyclerView.Adapter<ScheduleCalendarAdapter.ViewHolder>() {

    companion object {
        const val TAG = "SCAdapter"
        const val ViewTypeTimeScale = 100
        const val ViewTypeDateLabel = 101
        const val ViewTypeCurrentTime = 102
        const val ViewTypeSchedule = 110
    }

    private val items: MutableList<ScheduleItem> = mutableListOf()

    abstract fun createScheduleViewHolder(parent: ViewGroup): ViewHolder

    /**
     * Returns a [ScheduleItem] at given [position].
     *
     * @param position adapter position
     */
    fun getItem(position: Int): ScheduleItem? {
        return items.getOrNull(position)
    }

    fun addItem(item: ScheduleItem) {
        // TODO add date label if necessary?
        // add item
        val position = findListPositionToBeInserted(item)
        this.items.add(position, item)
        notifyItemInserted(position)
    }

    private fun findListPositionToBeInserted(item: ScheduleItem): Int {
        return items.indexOfFirst { scheduleItem ->
            item.compareTo(scheduleItem) < 0
        }.let { if (it == -1) items.size else it }
    }

    fun addItems(vararg items: ScheduleItem) {
        val list = items.toList().sort()
        val firstItem = list.firstOrNull() ?: return
        val lastItem = list.lastOrNull() ?: return
        // first, add only 'dateLabel' items
        if (this.items.isEmpty()) {
            val dateLabels = DateScheduleItem.fromDate(firstItem.start()).nextDays(lastItem.start(), true)
            this.items.addAll(dateLabels)
            notifyItemRangeInserted(0, dateLabels.size)
        } else {
            if (getFirstDateScheduleItem()?.start()?.after(firstItem.start()) == true) {
                addPreviousDateLabelItems(firstItem.start())
            }
            if (getLastDateScheduleItem()?.start()?.before(lastItem.start()) == true) {
                addFollowingDateLabelItems(lastItem.start())
            }
        }

        // next, add other schedule items.
        list.filterNotDateScheduleItems().forEach {
            addItem(it)
        }
    }

    fun addPreviousDateLabelItems(count: Int) {
        getFirstDateScheduleItem()?.let { dateItem ->
            val previousDays = dateItem.previousDays(count)
            items.addAll(0, previousDays)
            notifyItemRangeInserted(0, previousDays.size)
        }
    }

    fun addPreviousDateLabelItems(date: Date) {
        getFirstDateScheduleItem()?.let { dateItem ->
            if (dateItem.start().after(date)) {
                val previousDays = dateItem.previousDays(dateItem.start().dateDiff(date))
                items.addAll(0, previousDays)
                notifyItemRangeInserted(0, previousDays.size)
            }
        }
    }

    fun addFollowingDateLabelItems(count: Int) {
        getLastDateScheduleItem()?.let { dateItem ->
            val followingDays = dateItem.nextDays(count)
            val position = items.size
            items.addAll(position, followingDays)
            notifyItemRangeInserted(position, followingDays.size)
        }
    }

    fun addFollowingDateLabelItems(date: Date) {
        getLastDateScheduleItem()?.let { dateItem ->
            if (dateItem.start().before(date)) {
                val followingDays = dateItem.nextDays(date.dateDiff(dateItem.start()))
                val position = items.size
                items.addAll(position, followingDays)
                notifyItemRangeInserted(position, followingDays.size)
            }
        }
    }

    override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
    ): ViewHolder {
        return when (viewType) {
            ViewTypeTimeScale -> TimeScaleViewHolder(TimeScaleView(parent.context))
            ViewTypeDateLabel -> DateLabelViewHolder(DateLabelView(parent.context))
            ViewTypeCurrentTime -> CurrentTimeViewHolder(CurrentTimeView(parent.context))
            ViewTypeSchedule -> createScheduleViewHolder(parent)
            else -> throw IllegalArgumentException("unknown view type: ${viewType}")
        }
    }


    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        when {
            position >= items.size -> Unit
            else -> {
                holder.bind(items[position])
            }
        }
    }

    override fun getItemCount(): Int {
        return items.size + FIX_VIEW_OFFSET
    }

    override fun getItemViewType(position: Int): Int {
        return when (position) {
            items.size -> ViewTypeTimeScale
            else -> {
                when (items.getOrNull(position)) {
                    is DateScheduleItem -> ViewTypeDateLabel
                    is CurrentTimeScheduleItem -> ViewTypeCurrentTime
                    else -> ViewTypeSchedule
                }
            }
        }
    }

    /**
     * Returns the first [DateScheduleItem] in the list.
     */
    fun getFirstDateScheduleItem(): DateScheduleItem? {
        for (i in 0 until items.size) {
            items.getOrNull(i)?.let { item ->
                if (item is DateScheduleItem) return item
            }
        }
        return null
    }

    /**
     * Returns the last [DateScheduleItem] in the list.
     */
    fun getLastDateScheduleItem(): DateScheduleItem? {
        for (i in items.size - 1 downTo 0) {
            items.getOrNull(i)?.let { item ->
                if (item is DateScheduleItem) return item
            }
        }
        return null
    }

    abstract class ViewHolder(itemView: View) :
            RecyclerView.ViewHolder(itemView) {

        abstract fun bind(item: ScheduleItem)
    }
}