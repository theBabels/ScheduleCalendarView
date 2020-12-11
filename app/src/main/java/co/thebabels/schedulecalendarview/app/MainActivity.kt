package co.thebabels.schedulecalendarview.app

import android.app.Activity
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import co.thebabels.schedulecalendarview.*
import java.util.*

class MainActivity : AppCompatActivity() {

    private val lm = GridLayoutManager(this, 2)
    private val adapter = TextScheduleAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val recyclerView: RecyclerView = findViewById(R.id.recycler_view)
        val layoutListener = LayoutListener(this)
        recyclerView.layoutManager =
            ScheduleCalendarLayoutManager(this).apply {
                setDateLookUp(DefaultDateLookUp(adapter))
                setListener(layoutListener)
            }
        recyclerView.adapter = adapter
//        val snapHelper = PagerSnapHelper()
//        snapHelper.attachToRecyclerView(recyclerView)


        val cal = Calendar.getInstance()
        cal.apply {
            set(Calendar.HOUR_OF_DAY, 8)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val days = DateScheduleItem.firstDayOfWeek().nextDays(60, true)
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
        )
    }

    data class TextScheduleItem(val text: String, val start: Date, val end: Date) : ScheduleItem {
        override fun start(): Date {
            return this.start
        }

        override fun end(): Date {
            return this.end
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

    private class LayoutListener(val a: Activity) : ScheduleCalendarLayoutManager.Listener {
        override fun onFirstItemChanged(position: Int, date: Date) {
            val monthTextView: TextView = a.findViewById(R.id.month_text)
            monthTextView.text = Calendar.getInstance().apply { time = date }
                .getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.getDefault())
        }
    }
}