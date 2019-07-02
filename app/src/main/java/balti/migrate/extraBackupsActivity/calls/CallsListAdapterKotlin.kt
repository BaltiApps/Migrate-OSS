package balti.migrate.extraBackupsActivity.calls

import android.content.Context
import android.provider.CallLog
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import balti.migrate.R
import kotlinx.android.synthetic.main.calls_item.view.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.Comparator

class CallsListAdapterKotlin(val context: Context,
                             private val callsList: ArrayList<CallsDataPacketsKotlin>): BaseAdapter() {

    init {
        callsList.sortWith(Comparator { t1, t2 -> String.CASE_INSENSITIVE_ORDER.compare(t1.callsDate.toString(), t2.callsDate.toString()) })
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {

        val cdp = callsList[position]

        val view = View.inflate(context, R.layout.calls_item, null)

        view.calls_item_checkbox.apply {
            isChecked = cdp.selected
            setOnCheckedChangeListener { _, isChecked ->
                cdp.selected = isChecked
            }
        }

        cdp.callsCachedName.let {name ->
            if (name != null && name != ""){
                view.calls_item_person.text = name
                view.calls_item_number.apply {
                    visibility = View.VISIBLE
                    text = cdp.callsNumber
                }
            }
            else {
                view.calls_item_number.visibility = View.GONE
                view.calls_item_person.text = cdp.callsNumber
            }
        }

        view.calls_item_date.text = getDate(cdp.callsDate)
        view.calls_item_duration.text = getDuration(cdp.callsDuration)
        view.setOnClickListener {
            view.calls_item_checkbox.isChecked != view.calls_item_checkbox.isChecked
        }

        view.calls_item_icon.setImageResource(
                when (cdp.callsType){
                    "${CallLog.Calls.INCOMING_TYPE}" -> R.drawable.ic_incoming_call
                    "${CallLog.Calls.OUTGOING_TYPE}" -> R.drawable.ic_outgoing_call
                    "${CallLog.Calls.MISSED_TYPE}" -> R.drawable.ic_missed_call
                    else -> R.drawable.ic_call_log_icon
                }
        )
        
        return view
    }


    fun checkAll(isChecked: Boolean){
        for (cdp in callsList)
            cdp.selected = isChecked
        notifyDataSetChanged()
    }

    private fun getDate(l: Long): String = SimpleDateFormat("dd/MM/yyyy").format(Date(l))

    private fun getDuration(l: Long): String {
        var l = l
        var duration = ""

        try {

            val d = l / (60 * 60 * 24)
            if (d != 0L) duration = "$duration$d days "
            l %= (60 * 60 * 24)

            val h = l / (60 * 60)
            if (h != 0L) duration = "$duration$h hrs "
            l %= (60 * 60)

            val m = l / 60
            if (m != 0L) duration = "$duration$m mins "
            l %= 60

            val s = l
            duration = "$duration$s secs"

        } catch (e: Exception) { e.printStackTrace() }

        return duration
    }

    override fun getItem(position: Int): Any = callsList[position]
    override fun getItemId(position: Int): Long = 0
    override fun getCount(): Int = callsList.size
    override fun getViewTypeCount(): Int = count
    override fun getItemViewType(position: Int): Int = position
}