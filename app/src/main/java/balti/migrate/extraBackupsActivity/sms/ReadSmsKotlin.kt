package balti.migrate.extraBackupsActivity.sms

import android.content.Context
import android.database.Cursor
import android.os.AsyncTask
import android.view.View
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import balti.migrate.R
import balti.migrate.extraBackupsActivity.OnJobCompletion
import balti.migrate.extraBackupsActivity.ViewOperations
import balti.migrate.utilities.extraBackupsTools.SmsToolsKotlin

class ReadSmsKotlin(private val jobCode: Int,
                    private val context: Context,
                    private val menuMainItem: LinearLayout,
                    private val menuSelectedStatus: TextView,
                    private val menuReadProgressBar: ProgressBar,
                    private val doBackupCheckbox: CheckBox
                    ) : AsyncTask<Any, Any, ArrayList<SmsDataPacketKotlin>>() {

    private var smsCount = 0
    private val smsTools by lazy { SmsToolsKotlin(context) }

    private val inboxCursor: Cursor? by lazy { smsTools.getSmsInboxCursor() }
    private val outboxCursor: Cursor? by lazy { smsTools.getSmsOutboxCursor() }
    private val sentCursor: Cursor? by lazy { smsTools.getSmsSentCursor() }
    private val draftCursor: Cursor? by lazy { smsTools.getSmsDraftCursor() }

    private val vOp by lazy { ViewOperations(context) }
    private val onJobCompletion by lazy { context as OnJobCompletion }
    private var error = ""
    private var isSmsChecked = false

    override fun onPreExecute() {
        super.onPreExecute()
        vOp.visibilitySet(menuSelectedStatus, View.VISIBLE)
        vOp.visibilitySet(menuReadProgressBar, View.VISIBLE)

        if (inboxCursor != null || outboxCursor != null || sentCursor != null || draftCursor != null){

            inboxCursor?.let {
                smsCount += it.count
            }

            outboxCursor?.let {
                smsCount += it.count
            }

            sentCursor?.let {
                smsCount += it.count
            }

            draftCursor?.let {
                smsCount += it.count
            }

            vOp.progressSet(menuReadProgressBar, 0, smsCount)
            vOp.enableSet(doBackupCheckbox, true)
        }
        else {
            vOp.enableSet(doBackupCheckbox, false)
            vOp.checkSet(doBackupCheckbox, false)
            vOp.textSet(menuSelectedStatus, R.string.reading_error)
            vOp.visibilitySet(menuReadProgressBar, View.GONE)
        }
        vOp.clickableSet(menuMainItem, false)
        vOp.doSomething { isSmsChecked = doBackupCheckbox.isChecked }
    }

    override fun doInBackground(vararg params: Any?): ArrayList<SmsDataPacketKotlin> {

        val tmpList = ArrayList<SmsDataPacketKotlin>(0)
        var c = 0

        fun collectMessagesFromCursor(cursor: Cursor?): String {
            cursor?.let {cursorItem ->
                if (cursor.count > 0) cursorItem.moveToFirst()
                for (i in 0 until cursor.count){
                    smsTools.getSmsPacket(cursorItem, isSmsChecked).let {cdp ->
                        smsTools.errorEncountered.trim().let { err ->
                            if (err == "") {
                                tmpList.add(cdp)
                                publishProgress(c, "${vOp.getStringFromRes(R.string.reading_sms)}\n${c++}")
                                cursorItem.moveToNext()
                            } else return (err)
                        }
                    }
                }
            }
            return ""
        }


        try {

            collectMessagesFromCursor(inboxCursor).let { if (it.trim() != "") throw Exception(it) }
            collectMessagesFromCursor(outboxCursor).let { if (it.trim() != "") throw Exception(it) }
            collectMessagesFromCursor(sentCursor).let { if (it.trim() != "") throw Exception(it) }
            collectMessagesFromCursor(draftCursor).let { if (it.trim() != "") throw Exception(it) }

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

    override fun onPostExecute(result: ArrayList<SmsDataPacketKotlin>?) {
        super.onPostExecute(result)

        if (error == "") onJobCompletion.onComplete(jobCode, true, result)
        else onJobCompletion.onComplete(jobCode, false, error)

        inboxCursor?.let { try { it.close() } catch (_ : Exception){} }
        outboxCursor?.let { try { it.close() } catch (_ : Exception){} }
        sentCursor?.let { try { it.close() } catch (_ : Exception){} }
        draftCursor?.let { try { it.close() } catch (_ : Exception){} }

        vOp.clickableSet(menuMainItem, smsCount > 0)
    }

}