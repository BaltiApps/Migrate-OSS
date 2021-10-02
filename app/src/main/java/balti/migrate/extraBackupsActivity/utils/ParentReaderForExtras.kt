package balti.migrate.extraBackupsActivity.utils

import android.util.Log
import android.view.View
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import balti.migrate.AppInstance
import balti.migrate.utilities.CommonToolsKotlin.Companion.DEBUG_TAG
import balti.module.baltitoolbox.functions.Misc.tryIt
import balti.module.baltitoolbox.jobHandlers.AsyncCoroutineTask

abstract class ParentReaderForExtras(private val fragment: ParentFragmentForExtras): AsyncCoroutineTask() {

    val context by lazy { AppInstance.appContext }

    companion object {
        /**
         * List of class names of reader classes which are actively reading data.
         * Just before reading starts, the name of the class is added in the list in [onPreExecute].
         * After reading finishes, the name of the class is removed in [onPostExecute].
         *
         * If this list is empty, only the "Start backup" button is un-hidden.
         * If this list is not empty, it means some other reader class is still reading some data,
         * hence "Start backup" button is not un-hidden.
         */
        val inProcessReaderClasses by lazy { ArrayList<String>(0) }
    }

    fun writeLog(message: String){
        Log.d(DEBUG_TAG, "$className: $message")
    }

    val mainItem: LinearLayout? get() = fragment.delegateMainItem
    val readStatusText: TextView? get() = fragment.delegateStatusText
    val readProgressBar: ProgressBar? get() = fragment.delegateProgressBar
    val doBackupCheckBox: CheckBox? get() = fragment.delegateCheckbox

    final override suspend fun onPreExecute() {
        super.onPreExecute()
        fragment.delegateBackupButtonWaiting?.visibility = View.VISIBLE
        fragment.delegateStartBackupButton?.visibility = View.GONE
        tryIt { inProcessReaderClasses.add(className) }
        preExecute()
    }

    final override suspend fun doInBackground(arg: Any?): Any {
        return backgroundProcessing()
    }

    final suspend fun executeWithResult(): ReaderJobResultHolder {
        return super.executeWithResult(null) as ReaderJobResultHolder
    }

    final override suspend fun onPostExecute(result: Any?) {
        super.onPostExecute(result)
        postExecute(result)

        /**
         * Variable to store if the list is empty after removing reader class name.
         * By default value is true. So if there is any error in checking the list,
         * the "Start backup" button will be un-hidden anyway, so that the user is not left hanging.
         * In case of error, by the time app storage space is calculated, hopefully, all data will be read.
         */
        var inProcessListEmptyAfterRemoval = true
        tryIt {
            inProcessReaderClasses.remove(className)
            inProcessListEmptyAfterRemoval = inProcessReaderClasses.isEmpty()
        }
        if (inProcessListEmptyAfterRemoval) {
            fragment.delegateBackupButtonWaiting?.visibility = View.GONE
            fragment.delegateStartBackupButton?.visibility = View.VISIBLE
        }
    }

    abstract val className: String
    abstract suspend fun backgroundProcessing(): ReaderJobResultHolder
    abstract fun preExecute()
    abstract fun postExecute(result: Any?)

}