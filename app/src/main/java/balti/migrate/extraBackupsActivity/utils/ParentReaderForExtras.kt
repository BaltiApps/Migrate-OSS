package balti.migrate.extraBackupsActivity.utils

import android.util.Log
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import balti.migrate.AppInstance
import balti.migrate.utilities.CommonToolsKotlin.Companion.DEBUG_TAG
import balti.module.baltitoolbox.jobHandlers.AsyncCoroutineTask

abstract class ParentReaderForExtras(private val fragment: ParentFragmentForExtras): AsyncCoroutineTask() {

    val context by lazy { AppInstance.appContext }

    fun writeLog(message: String){
        Log.d(DEBUG_TAG, "$className: $message")
    }

    val mainItem: LinearLayout? get() = fragment.delegateMainItem
    val readStatusText: TextView? get() = fragment.delegateStatusText
    val readProgressBar: ProgressBar? get() = fragment.delegateProgressBar
    val doBackupCheckBox: CheckBox? get() = fragment.delegateCheckbox

    final override suspend fun doInBackground(arg: Any?): Any {
        return backgroundProcessing()
    }

    final suspend fun executeWithResult(): ReaderJobResultHolder {
        return super.executeWithResult(null) as ReaderJobResultHolder
    }

    abstract val className: String
    abstract suspend fun backgroundProcessing(): ReaderJobResultHolder


}