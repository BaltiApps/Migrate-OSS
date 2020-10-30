package balti.migrate.messages

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import balti.migrate.R
import balti.migrate.utilities.CommonToolsKotlin.Companion.EXTRA_MESSAGE_CONTENT
import balti.migrate.utilities.CommonToolsKotlin.Companion.MESSAGE_FIELD_LAST_UPDATE_NO
import balti.migrate.utilities.CommonToolsKotlin.Companion.MESSAGE_FIELD_MESSAGE_ARRAY
import balti.migrate.utilities.CommonToolsKotlin.Companion.PREF_LAST_MESSAGE_LEVEL
import balti.module.baltitoolbox.functions.Misc.doBackgroundTask
import balti.module.baltitoolbox.functions.Misc.tryIt
import balti.module.baltitoolbox.functions.SharedPrefs.putPrefInt
import kotlinx.android.synthetic.main.messages_activity.*
import org.json.JSONObject

class MessagesView: AppCompatActivity() {

    private val messages by lazy { ArrayList<JSONObject>(0) }
    private var updateNumber = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.messages_activity)

        if (intent.hasExtra(EXTRA_MESSAGE_CONTENT)) {
            tryIt {
                val json = JSONObject(intent.getStringExtra(EXTRA_MESSAGE_CONTENT))
                updateNumber = json.getInt(MESSAGE_FIELD_LAST_UPDATE_NO)
                messages.clear()
                val arr = json.getJSONArray(MESSAGE_FIELD_MESSAGE_ARRAY)
                for (i in 0 until arr.length()){
                    messages.add(arr.getJSONObject(i))
                }
            }
        }

        if (messages.isEmpty()){
            finish()
            Toast.makeText(this, R.string.no_messages, Toast.LENGTH_SHORT).show()
        }
        else {
            var adapter: MessageAdapter? = null
            doBackgroundTask({
                tryIt {
                    adapter = MessageAdapter(this, messages, updateNumber)
                }
                return@doBackgroundTask 0
            }, {
                tryIt {
                    if (adapter != null)
                        messages_list.adapter = adapter
                }
            })
        }

        messages_close.setOnClickListener {
            onBackPressed()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        tryIt { putPrefInt(PREF_LAST_MESSAGE_LEVEL, updateNumber) }
    }
}