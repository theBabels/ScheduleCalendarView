package co.thebabels.schedulecalendarview.app

import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import co.thebabels.schedulecalendarview.*
import co.thebabels.schedulecalendarview.extention.toCalendar
import com.google.android.material.snackbar.Snackbar
import java.util.*

class MainActivity : AppCompatActivity() {

    private val adapter = TextScheduleAdapter()
    private val scrollListener = ScrollListener()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val recyclerView: RecyclerView = findViewById(R.id.recycler_view)
        recyclerView.layoutManager =
                ScheduleCalendarLayoutManager(this).apply {
                    setDateLookUp(DefaultDateLookUp(adapter))
                    setListener(LayoutListener())
                }
        recyclerView.adapter = adapter
        recyclerView.addOnScrollListener(scrollListener)
//        val snapHelper = PagerSnapHelper()
//        snapHelper.attachToRecyclerView(recyclerView)

        val todayButton: Button = findViewById(R.id.today_btn)
        todayButton.setOnClickListener {
            Log.d("MainActivity", "onClickTodayButton")
            adapter.getDateLabelPosition()?.let {
                recyclerView.smoothScrollToPosition(it)
            }
        }

        val deleteButton: Button = findViewById(R.id.delete_btn)


        val cal = Calendar.getInstance()
        cal.apply {
            set(Calendar.HOUR_OF_DAY, 8)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val days = DateScheduleItem.firstDayOfWeek().nextDays(90, true)
        Log.d("DEBUG", "${days.map { it.dateString() }}")
        adapter.addItems(
                *days.toTypedArray(),
                TextScheduleItem(
                        "text-000",
                        cal.time,
                        cal.apply { add(Calendar.HOUR, 1) }.time,
                ),
                TextScheduleItem(
                        "text-001",
                        cal.apply { add(Calendar.HOUR, 1) }.time,
                        cal.apply { add(Calendar.HOUR, 1) }.time
                ),
                TextScheduleItem(
                        "text-002",
                        cal.apply { add(Calendar.DATE, 1) }.time,
                        cal.apply { add(Calendar.HOUR, 4) }.time
                ),
                TextScheduleItem(
                        "text-003",
                        cal.apply { add(Calendar.DATE, 1) }.time,
                        cal.apply { add(Calendar.HOUR, 1) }.time
                ),
                TextScheduleItem(
                        "text-004",
                        cal.apply { add(Calendar.DATE, 2) }.time,
                        cal.apply { add(Calendar.HOUR, 24) }.time
                ),
                TextScheduleItem(
                        "Fill Item",
                        cal.apply { add(Calendar.HOUR, 2) }.time,
                        cal.apply { add(Calendar.HOUR, 4) }.time,
                        isFill = true,
                ),
                CurrentTimeScheduleItem.now(),
        )

        val touchHelper =
                ScheduleCalendarItemTouchHelper(object : ScheduleCalendarItemTouchHelper.Callback() {
                    override fun isEditable(viewHolder: RecyclerView.ViewHolder): Boolean {
                        return viewHolder is TextScheduleAdapter.ViewHolder
                    }

                    override fun onMove(
                            recyclerView: RecyclerView,
                            viewHolder: RecyclerView.ViewHolder,
                            start: Date,
                            end: Date
                    ): Boolean {
                        adapter.updateItem(viewHolder.adapterPosition, start, end)
                        return true
                    }

                    override fun getScheduleItem(position: Int): ScheduleItem? {
                        return adapter.getItem(position)
                    }

                    override fun onSelected(adapterPosition: Int) {
                        // enabl delete button
                        deleteButton.isEnabled = true
                    }

                    override fun onSelectionFinished(adapterPosition: Int?, prev: ScheduleItem?) {
                        val view: View = findViewById(R.id.recycler_view)
                        if (adapterPosition != null && adapter.getItem(adapterPosition) != prev) {
                            Snackbar.make(
                                    view,
                                    R.string.snackbar_selection_finished,
                                    Snackbar.LENGTH_LONG
                            )
                                    .let {
                                        prev?.let { before ->
                                            it.setAction(R.string.snackbar_action_recover) {
                                                adapter.updateItem(
                                                        adapterPosition,
                                                        before.start(),
                                                        before.end()
                                                )
                                            }
                                        } ?: it
                                    }
                                    .show()
                        }
                        // disable delete button
                        deleteButton.isEnabled = false
                    }

                    override fun createItem(date: Date): Int? {
                        return adapter.addItem(
                                TextScheduleItem(
                                        getTextLabel(),
                                        date,
                                        date.toCalendar().apply { add(Calendar.HOUR, 1) }.time,
                                        null,
                                        false,
                                )
                        )
                    }
                })
        touchHelper.attachToRecyclerView(recyclerView)

        deleteButton.setOnClickListener {
            touchHelper.selected?.adapterPosition?.let { pos ->
                adapter.deleteItems(pos)
            }
        }
    }

    private fun getTextLabel(): String {
        return Calendar.getInstance().let { cal ->
            val hour = cal.get(Calendar.HOUR_OF_DAY)
            val minute = cal.get(Calendar.MINUTE)
            val sec = cal.get(Calendar.SECOND)
            return "text-${hour}:${minute}:${sec}"
        }
    }

    data class TextScheduleItem(
            val text: String,
            val start: Date,
            val end: Date,
            private var origin: ScheduleItem? = null,
            private val isFill: Boolean = false
    ) : ScheduleItem {

        override fun key(): String {
            return text
        }

        override fun start(): Date {
            return this.start
        }

        override fun end(): Date {
            return this.end
        }

        override fun getOrigin(): ScheduleItem? {
            return this.origin
        }

        override fun setOrigin(origin: ScheduleItem?) {
            this.origin = origin
        }

        override fun update(start: Date, end: Date): ScheduleItem {
            return this.copy(start = start, end = end)
        }

        override fun isFillItem(): Boolean {
            return isFill
        }
    }


    class TextScheduleAdapter() : ScheduleCalendarAdapter() {

        override fun createScheduleViewHolder(
                viewType: Int,
                parent: ViewGroup
        ): ScheduleCalendarAdapter.ViewHolder {
            return ViewHolder(TextView(parent.context).apply {
                setBackgroundResource(R.drawable.bg_text_item)
                setPadding(8, 8, 8, 8)
            })
        }

        class ViewHolder(itemView: View) : ScheduleCalendarAdapter.ViewHolder(itemView) {

            override fun bind(item: ScheduleItem) {
                if (itemView is TextView) {
                    if (item is TextScheduleItem) {
                        itemView.text = item.text
                    }
                }
                if (item.isFillItem()) {
                    itemView.setBackgroundColor(Color.GRAY)
                } else {
                    itemView.setBackgroundResource(R.drawable.bg_text_item)
                }
            }
        }
    }

    private inner class LayoutListener() : ScheduleCalendarLayoutManager.Listener {
        override fun onFirstItemChanged(position: Int, date: Date) {
            val monthTextView: TextView = findViewById(R.id.month_text)
            monthTextView.text = Calendar.getInstance().apply { time = date }
                    .let {
                        "${it.get(Calendar.YEAR)}." +
                                it.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.getDefault())
                    }
        }
    }

    private inner class ScrollListener : RecyclerView.OnScrollListener() {

        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            Log.d("onScrolled", "${recyclerView.computeVerticalScrollOffset()}/${recyclerView.computeVerticalScrollRange()}(${recyclerView.computeVerticalScrollExtent()})")
            // add date label items for infinity scroll.
            if (!recyclerView.isComputingLayout) {
                recyclerView.layoutManager?.let { lm ->
                    lm.getChildAt(0)?.let {
                        lm.getPosition(it)
                    }
                }?.let { adapterPosition ->
                    if (adapterPosition < 60 && dx < 0) {
                        recyclerView.post {
                            adapter.addPreviousDateLabelItems(30)
                        }
                    }
                    if (adapterPosition > adapter.itemCount - 60 && dx > 0) {
                        recyclerView.post {
                            adapter.addFollowingDateLabelItems(30)
                        }
                    }
                }
            }
        }
    }
}