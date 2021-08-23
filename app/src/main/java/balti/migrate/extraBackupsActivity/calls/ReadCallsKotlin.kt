package balti.migrate.extraBackupsActivity.calls

import android.database.Cursor
import android.view.View
import balti.migrate.R
import balti.migrate.extraBackupsActivity.ParentReaderForExtras
import balti.migrate.extraBackupsActivity.ReaderJobResultHolder
import balti.migrate.extraBackupsActivity.calls.containers.CallsDataPacketsKotlin
import balti.migrate.extraBackupsActivity.calls.utils.CallsToolsKotlin
import balti.module.baltitoolbox.functions.GetResources.getStringFromRes
import balti.module.baltitoolbox.functions.Misc.tryIt

class ReadCallsKotlin(fragment: CallsFragment): ParentReaderForExtras(fragment) {

    private var callsCount = 0
    private val callsTools by lazy { CallsToolsKotlin(context) }

    private val cursor: Cursor? by lazy { callsTools.getCallsCursor() }

    private var error = ""
    private var isCallsChecked = false

    override val className: String = "ReadCallsKotlin"

    override suspend fun onPreExecute() {
        super.onPreExecute()
        readStatusText?.visibility = View.VISIBLE
        readProgressBar?.visibility = View.VISIBLE
        tryIt {
            cursor?.let {
                callsCount = it.count
                readProgressBar?.apply {
                    max = callsCount
                    progress = 0
                }
                doBackupCheckBox?.isEnabled = true
            }
            if (cursor == null) {
                doBackupCheckBox?.isChecked = false
                readStatusText?.setText(R.string.reading_error)
                readProgressBar?.visibility = View.GONE
            }
        }
        mainItem?.isClickable = false
        isCallsChecked = doBackupCheckBox?.isChecked ?: false
    }


    override suspend fun doInBackground(arg: Any?): Any {

        writeLog("Starting reading")

        val tmpList = ArrayList<CallsDataPacketsKotlin>(0)
        try {
            cursor?.let {cursorItem ->
                if (callsCount > 0) cursorItem.moveToFirst()
                for (i in 0 until callsCount){

                    if (doBackupCheckBox?.isChecked != true) {
                        writeLog("Break reading calls")
                        break
                    }

                    callsTools.getCallsPacket(cursorItem, isCallsChecked).let {cdp ->
                        callsTools.errorEncountered.trim().let { err ->
                            if (err == "") {
                                tmpList.add(cdp)
                                publishProgress(i, "${getStringFromRes(R.string.reading_calls)}\n$i")
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

        cursor?.let {
            tryIt { it.close() }
        }

        return if (error == "") {
            doOnMainThreadParallel {
                mainItem?.isClickable = true
            }
            writeLog("Read success. Read - ${tmpList.size}")
            ReaderJobResultHolder(true, tmpList)
        } else {
            writeLog("Read fail. Error - $error")
            ReaderJobResultHolder(false, error)
        }
    }

    override suspend fun onProgressUpdate(vararg values: Any) {
        super.onProgressUpdate(*values)
        readProgressBar?.progress = values[0] as Int
        readStatusText?.text = values[1] as String
    }
}