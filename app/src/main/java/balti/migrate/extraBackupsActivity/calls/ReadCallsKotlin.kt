package balti.migrate.extraBackupsActivity.calls

import android.content.Context
import android.database.Cursor
import android.os.AsyncTask
import android.view.View
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import balti.migrate.R
import balti.migrate.extraBackupsActivity.calls.containers.CallsDataPacketsKotlin
import balti.migrate.extraBackupsActivity.calls.utils.CallsToolsKotlin
import balti.migrate.extraBackupsActivity.utils.OnJobCompletion
import balti.migrate.extraBackupsActivity.utils.ViewOperations

class ReadCallsKotlin(private val jobCode: Int,
                      private val context: Context,
                      private val menuMainItem: LinearLayout,
                      private val menuSelectedStatus: TextView,
                      private val menuReadProgressBar: ProgressBar,
                      private val doBackupCheckbox: CheckBox
                      ) : AsyncTask<Any, Any, ArrayList<CallsDataPacketsKotlin>>() {

    private var callsCount = 0
    private val callsTools by lazy { CallsToolsKotlin(context) }

    private val cursor: Cursor? by lazy { callsTools.getCallsCursor() }

    private val vOp by lazy { ViewOperations(context) }
    private val onJobCompletion by lazy { context as OnJobCompletion }
    private var error = ""
    private var isCallsChecked = false

    override fun onPreExecute() {
        super.onPreExecute()
        vOp.visibilitySet(menuSelectedStatus, View.VISIBLE)
        vOp.visibilitySet(menuReadProgressBar, View.VISIBLE)
        vOp.doSomething {
            cursor?.let {
                callsCount = it.count
                vOp.progressSet(menuReadProgressBar, 0, callsCount)
                vOp.enableSet(doBackupCheckbox, true)
            }
            if (cursor == null) {
                vOp.checkSet(doBackupCheckbox, false)
                vOp.textSet(menuSelectedStatus, R.string.reading_error)
                vOp.visibilitySet(menuReadProgressBar, View.GONE)
            }
        }
        vOp.clickableSet(menuMainItem, false)
        vOp.doSomething { isCallsChecked = doBackupCheckbox.isChecked }
    }


    override fun doInBackground(vararg params: Any?): ArrayList<CallsDataPacketsKotlin> {
        val tmpList = ArrayList<CallsDataPacketsKotlin>(0)
        try {
            cursor?.let {cursorItem ->
                if (callsCount > 0) cursorItem.moveToFirst()
                for (i in 0 until callsCount){
                    callsTools.getCallsPacket(cursorItem, isCallsChecked).let {cdp ->
                        callsTools.errorEncountered.trim().let { err ->
                            if (err == "") {
                                tmpList.add(cdp)
                                publishProgress(i, "${vOp.getStringFromRes(R.string.reading_calls)}\n$i")
                                cursorItem.moveToNext()
                            } else throw Exception(err)
                        }
                    }
                }
            }
        } catch (e: Exception){
            error = e.message.toString()
            e.printStackTrace()
        }
        return tmpList
    }

    override fun onProgressUpdate(vararg values: Any?) {
        super.onProgressUpdate(*values)
        vOp.progressSet(menuReadProgressBar, values[0] as Int)
        vOp.textSet(menuSelectedStatus, values[1] as String)
    }

    override fun onPostExecute(result: ArrayList<CallsDataPacketsKotlin>?) {
        super.onPostExecute(result)

        vOp.doSomething {

            if (error == "") {
                vOp.clickableSet(menuMainItem, callsCount > 0)
                onJobCompletion.onComplete(jobCode, true, result)
            }
            else onJobCompletion.onComplete(jobCode, false, error)

            cursor?.let {
                vOp.doSomething { it.close() }
            }

        }
    }
}