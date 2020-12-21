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
import co.thebabels.schedulecalendarview.extention.clearToMidnight
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
                CurrentTimeScheduleItem.now(),
        )

        val touchHelper = ScheduleCalendarItemTouchHelper(object : ScheduleCalendarItemTouchHelper.Callback() {
            override fun isEditable(viewHolder: RecyclerView.ViewHolder): Boolean {
                return viewHolder is TextScheduleAdapter.ViewHolder
            }

            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, start: Date, end: Date): Boolean {
                adapter.updateItem(viewHolder.adapterPosition, start, end)
                return true
            }
        })
        touchHelper.attachToRecyclerView(recyclerView)
    }

    data class TextScheduleItem(val text: String, val start: Date, val end: Date) : ScheduleItem {
        override fun start(): Date {
            return this.start
        }

        override fun end(): Date {
            return this.end
        }

        override fun update(start: Date, end: Date): ScheduleItem {
            return this.copy(start = start, end = end)
        }
    }


    class TextScheduleAdapter() : ScheduleCalendarAdapter() {

        override fun createScheduleViewHolder(parent: ViewGroup): ScheduleCalendarAdapter.ViewHolder {
            return ViewHolder(TextView(parent.context).apply {
                setBackgroundColor(Color.GRAY)
                setPadding(8, 8, 8, 8)
            })
        }

        class ViewHolder(itemView: View) :
                ScheduleCalendarAdapter.ViewHolder(itemView) {

            override fun bind(item: ScheduleItem) {
                if (itemView is TextView) {
                    if (item is TextScheduleItem) {
                        itemView.text = item.text
                    }
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