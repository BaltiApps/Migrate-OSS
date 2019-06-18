package balti.migrate.extraBackupsActivity.sms

import android.content.Context
import android.provider.Telephony
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import balti.migrate.R
import kotlinx.android.synthetic.main.sms_item.view.*
import java.util.ArrayList
import kotlin.Any
import kotlin.Boolean
import kotlin.Comparator
import kotlin.Int
import kotlin.Long
import kotlin.String
import kotlin.apply

class SmsListAdapterKotlin(val context: Context,
                           private val smsList: ArrayList<SmsDataPacketKotlin>): BaseAdapter() {

    init {
        smsList.sortWith(Comparator { t1, t2 -> String.CASE_INSENSITIVE_ORDER.compare(t1.smsThreadID, t2.smsThreadID) })
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {

        val sdp = smsList[position]

        val view = View.inflate(context, R.layout.sms_item, null)

        view.sms_item_checkbox.apply {
            isChecked = sdp.selected
            setOnCheckedChangeListener { _, isChecked ->
                sdp.selected = isChecked
            }
        }

        view.sms_item_sender.text = sdp.smsAddress
        view.sms_item_body.text = sdp.smsBody
        view.setOnClickListener {
            view.sms_item_checkbox.isChecked != view.sms_item_checkbox.isChecked
        }

        view.sms_item_icon.setImageResource(
                when (sdp.smsType.toIntOrNull()){
                    Telephony.Sms.MESSAGE_TYPE_INBOX -> R.drawable.ic_incoming_sms
                    Telephony.Sms.MESSAGE_TYPE_SENT -> R.drawable.ic_outgoing_sms
                    Telephony.Sms.MESSAGE_TYPE_OUTBOX -> R.drawable.ic_outbox_sms
                    Telephony.Sms.MESSAGE_TYPE_DRAFT -> R.drawable.ic_draft_sms
                    else -> R.drawable.ic_sms_icon
                }
        )

        return view
    }

    fun checkAll(isChecked: Boolean){
        for (sdp in smsList)
            sdp.selected = isChecked
        notifyDataSetChanged()
    }

    override fun getItem(position: Int): Any = smsList[position]
    override fun getItemId(position: Int): Long = 0
    override fun getCount(): Int = smsList.size
    override fun getViewTypeCount(): Int = count
    override fun getItemViewType(position: Int): Int = position
}