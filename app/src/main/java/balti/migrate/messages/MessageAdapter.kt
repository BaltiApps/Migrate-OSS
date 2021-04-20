package balti.migrate.messages

import android.content.Context
import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Toast
import androidx.core.content.ContextCompat
import balti.migrate.R
import balti.migrate.utilities.CommonToolsKotlin.Companion.MESSAGE_FIELD_DATE
import balti.migrate.utilities.CommonToolsKotlin.Companion.MESSAGE_FIELD_MESSAGE_BODY
import balti.migrate.utilities.CommonToolsKotlin.Companion.MESSAGE_FIELD_MESSAGE_NO
import balti.migrate.utilities.CommonToolsKotlin.Companion.MESSAGE_FIELD_MESSAGE_TITLE
import balti.migrate.utilities.CommonToolsKotlin.Companion.MESSAGE_LINK
import balti.migrate.utilities.CommonToolsKotlin.Companion.PREF_LAST_MESSAGE_LEVEL
import balti.module.baltitoolbox.functions.Misc.openWebLink
import balti.module.baltitoolbox.functions.Misc.tryIt
import balti.module.baltitoolbox.functions.SharedPrefs.getPrefInt
import kotlinx.android.synthetic.main.messages_item.view.*
import org.json.JSONObject

class MessageAdapter(private val context: Context,
        private val messages: ArrayList<JSONObject>,
        private val updateNumber: Int): BaseAdapter() {


    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val v = View.inflate(context, R.layout.messages_item, null)
        try {
            val json = messages[position]

            tryIt { v.message_item_title.text = json.getString(MESSAGE_FIELD_MESSAGE_TITLE) }
            tryIt { v.message_item_body.text = json.getString(MESSAGE_FIELD_MESSAGE_BODY) }
            tryIt { v.message_item_date.text = json.getString(MESSAGE_FIELD_DATE) }

            v.setOnClickListener {
                tryIt {
                    arrayOf(v.message_item_title, v.message_item_body, v.message_item_date).forEach {
                        it.setBackgroundColor(Color.TRANSPARENT)
                    }
                    v.setBackgroundColor(Color.TRANSPARENT)
                    if (json.has(MESSAGE_LINK)) {
                        openWebLink(json.getString(MESSAGE_LINK))
                    }
                }
            }

            tryIt {
                if (json.getInt(MESSAGE_FIELD_MESSAGE_NO) == updateNumber &&
                        getPrefInt(PREF_LAST_MESSAGE_LEVEL, 0) < updateNumber) {
                    v.setBackgroundColor(ContextCompat.getColor(context, R.color.grey_bg))
                    arrayOf(v.message_item_title, v.message_item_body, v.message_item_date).forEach {
                        it.setBackgroundColor(ContextCompat.getColor(context, R.color.grey_bg))
                    }
                }
            }
        }
        catch (e: Exception){
            e.printStackTrace()
            Toast.makeText(context, e.message.toString(), Toast.LENGTH_SHORT).show()
        }

        return v
    }

    override fun getItem(position: Int): Any = messages[position]
    override fun getItemId(position: Int): Long = 0

    override fun getCount(): Int = messages.size
    override fun getViewTypeCount(): Int = count
    override fun getItemViewType(position: Int): Int = position

}