package balti.migrate.restore.data.sources.sms.dummies

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.widget.Toast
import balti.migrate.R

class DummyMmsBroadcastReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action != Telephony.Sms.Intents.WAP_PUSH_DELIVER_ACTION) return
        Toast.makeText(context, R.string.new_sms_received, Toast.LENGTH_LONG).show()
    }
}