package balti.migrate.extraBackupsActivity

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import balti.migrate.utilities.CommonToolsKotlin.Companion.DEBUG_TAG
import balti.module.baltitoolbox.functions.Misc.doBackgroundTask

abstract class ParentSelectorActivityForExtras(layoutId: Int): AppCompatActivity(layoutId) {

    final override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        setup()

        doBackgroundTask({
            backgroundProcessing()
        }, {
            postProcessing()
        })
    }

    final override fun onBackPressed() {
        sendResult(false)
    }

    fun sendResult(success: Boolean = false, data: Intent? = null){
        writeLog("Sending result. Success - $success")
        if (!success) {
            setResult(Activity.RESULT_CANCELED)
        }
        else {
            successBlock()
            if (data != null) setResult(Activity.RESULT_OK, data)
            else setResult(Activity.RESULT_OK)
        }
        finish()
    }

    fun writeLog(message: String){
        Log.d(DEBUG_TAG, "$className: $message")
    }

    abstract fun successBlock()
    abstract fun setup()
    abstract fun backgroundProcessing()
    abstract fun postProcessing()
    abstract val className: String
}