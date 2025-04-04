package balti.migrate.restore.data.sources.sms.dummies

import android.app.Activity
import android.os.Bundle
import android.widget.Toast
import balti.migrate.R

class DummyComposeSmsActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Toast.makeText(this, R.string.please_change_sms_app, Toast.LENGTH_LONG).show()
        finish()
    }
}
