package co.thebabels.schedulecalendarview

import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import co.thebabels.schedulecalendarview.extention.dateDiff
import co.thebabels.schedulecalendarview.extention.isToday
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

        const val PayloadMove = "Move"
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

    fun updateItem(position: Int, start: Date, end: Date) {
        // 1. update original item
        val item = getItem(position) ?: return
        val updatedItem = if (item.getOrigin() == null) {
            item.update(start, end)
        } else {
            item.reflectUpdateToOrigin(start, end)
        }

        // 2. find item positions with same key by given position.
        val oldItems = getItemsWithSameKey(position)

        // 3. split the updated original item
        val splitItems = updatedItem.splitAtMidnight()

        // 4. notify changes
        if (splitItems.size == oldItems.size) {
            Log.d(TAG, "updateItem(move):update=(${position}, ${item}, ${start}, ${end})")
            oldItems.forEachIndexed { index, oldItem ->
                val oldPos = items.indexOf(oldItem)
                items.removeAt(oldPos)
                val item = splitItems[index]
                val nextPos = findListPositionToBeInserted(item)
                items.add(nextPos, item)
                if (oldPos == nextPos) {
                    // Pass the 'payload'. Without this, the view will be completely updated, which will cause the view to be destroyed and the selection in ScheduleCalendarItemTouchHelper to be deselected.
                    // FIXME Should we have a dedicated method that is called from ItemTouchHelper.Callback?
                    notifyItemChanged(position, PayloadMove)
                } else {
                    notifyItemMoved(oldPos, nextPos)
                }
            }
        } else if (splitItems.size > oldItems.size) {
            val priorityIndex = splitItems.indexOfFirst { it.start().isToday(item.start()) }
            Log.d(TAG, "updateItem(insert):priorityIndex='${position}, ${priorityIndex}', sizeChanges='${oldItems.size}'->'${splitItems.size}', update=(${item}, ${start}, ${end})")
            splitItems.forEachIndexed { index, item ->
                val oldItemIndex = if (index < priorityIndex && index >= oldItems.size - 1) {
                    -1
                } else if (index == priorityIndex) {
                    oldItems.size - 1
                } else {
                    index
                }
                Log.d(TAG, "updateItem(insert):priorityIndex='${priorityIndex}', index='${index}' oldItemIndex='${oldItemIndex}', newItem='${item}'")
                oldItems.getOrNull(oldItemIndex)?.let { oldItem ->
                    val oldPos = items.indexOf(oldItem)
                    items.removeAt(oldPos)
                    val item = splitItems[index]
                    val nextPos = findListPositionToBeInserted(item)
                    items.add(nextPos, item)
                    if (oldPos == nextPos) {
                        // Pass the 'payload'. Without this, the view will be completely updated, which will cause the view to be destroyed and the selection in ScheduleCalendarItemTouchHelper to be deselected.
                        // FIXME Should we have a dedicated method that is called from ItemTouchHelper.Callback?
                        notifyItemChanged(position, PayloadMove)
                    } else {
                        notifyItemMoved(oldPos, nextPos)
                    }
                } ?: run {
                    val nextPos = findListPositionToBeInserted(item)
                    items.add(nextPos, item)
                    notifyItemInserted(nextPos)
                }
            }
        } else {
            // An old item at the specified position will be preferred as items to be moved, not deleted.
            val priorityIndex = oldItems.indexOf(item).let {
                if (it > splitItems.size - 1) it else -1
            }
            Log.d(TAG, "updateItem(remove):priorityIndex='${priorityIndex}', sizeChanges='${oldItems.size}'->'${splitItems.size}', update=(${position}, ${item}, ${start}, ${end})")
            oldItems.forEachIndexed { index, oldItem ->
                val oldPos = items.indexOf(oldItem)
                items.removeAt(oldPos)
                if (index < priorityIndex && index >= splitItems.size - 1) {
                    // If the item at the priority index may consume splitItems, simply delete the item instead of inserting and moving it.
                    notifyItemRemoved(oldPos)
                } else {
                    // For the priority index, select the last item;otherwise, use the index as is.
                    val splitItemIndex = if (index == priorityIndex) splitItems.size - 1 else index
                    splitItems.getOrNull(splitItemIndex)?.let { item ->
                        val nextPos = findListPositionToBeInserted(item)
                        items.add(nextPos, item)
                        if (oldPos == nextPos) {
                            // Pass the 'payload'. Without this, the view will be completely updated, which will cause the view to be destroyed and the selection in ScheduleCalendarItemTouchHelper to be deselected.
                            // FIXME Should we have a dedicated method that is called from ItemTouchHelper.Callback?
                            notifyItemChanged(position, PayloadMove)
                        } else {
                            notifyItemMoved(oldPos, nextPos)
                        }
                    } ?: run {
                        notifyItemRemoved(oldPos)
                    }
                }
            }
        }
    }

    fun getItemsWithSameKey(position: Int): List<ScheduleItem> {
        val item = getItem(position) ?: return listOf()
        val origin = item.getOrigin() ?: return listOf(item)
        val list = mutableListOf<ScheduleItem>()
        for (i in 0 until items.size) {
            val si = items[i]
            if (si.key() == origin.key()) {
                list.add(si)
            }
            if (si.start().after(origin.end())) {
                break
            }
        }
        return list
    }

    /**
     * Returns the position of [DateScheduleItem] with the same date as specified [date].
     *
     * @param date target date for scrolling. The default value is current time (=today).
     * @return the position of [DateScheduleItem] if it is found.
     *  'null' if [items] has no [DateScheduleItem].
     * '0' if given [date] is before than all [DateScheduleItem] in the [items].
     * '[items].size - 1' if given [date] is after than all [DateScheduleItem] in the [items].
     */
    fun getDateLabelPosition(date: Date = Date()): Int? {
        val first = items.firstOrNull { it is DateScheduleItem }?.start() ?: return null
        if (date.before(first)) {
            return 0
        }
        return items.indexOfFirst { it is DateScheduleItem && it.start().isToday(date) }.let {
            if (it == -1) {
                items.size - 1
            } else {
                it
            }
        }
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
            it.splitAtMidnight().forEach { splitItem -> addItem(splitItem) }
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
        onBindViewHolder(holder, position, mutableListOf())
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int, payloads: MutableList<Any>) {
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