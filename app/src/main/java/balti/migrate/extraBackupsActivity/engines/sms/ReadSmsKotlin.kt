package balti.migrate.extraBackupsActivity.engines.sms

import android.database.Cursor
import android.view.View
import balti.migrate.R
import balti.migrate.extraBackupsActivity.ParentReaderForExtras
import balti.migrate.extraBackupsActivity.ReaderJobResultHolder
import balti.migrate.extraBackupsActivity.engines.sms.containers.SmsDataPacketKotlin
import balti.migrate.extraBackupsActivity.engines.sms.utils.SmsToolsKotlin
import balti.module.baltitoolbox.functions.GetResources.getStringFromRes
import balti.module.baltitoolbox.functions.Misc.tryIt

class ReadSmsKotlin(fragment: SmsFragment): ParentReaderForExtras(fragment) {

    private var smsCount = 0
    private val smsTools by lazy { SmsToolsKotlin(context) }

    private val inboxCursor: Cursor? by lazy { smsTools.getSmsInboxCursor() }
    private val outboxCursor: Cursor? by lazy { smsTools.getSmsOutboxCursor() }
    private val sentCursor: Cursor? by lazy { smsTools.getSmsSentCursor() }
    private val draftCursor: Cursor? by lazy { smsTools.getSmsDraftCursor() }

    private var error = ""
    private var isSmsChecked = false

    override val className: String = "ReadSmsKotlin"

    override suspend fun onPreExecute() {
        super.onPreExecute()
        readStatusText?.visibility = View.VISIBLE
        readProgressBar?.visibility = View.VISIBLE
        tryIt {
            if (inboxCursor != null || outboxCursor != null || sentCursor != null || draftCursor != null) {

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

                readProgressBar?.apply {
                    max = smsCount
                    progress = 0
                }
                doBackupCheckBox?.isEnabled = true

            } else {
                doBackupCheckBox?.isChecked = false
                readStatusText?.setText(R.string.reading_error)
                readProgressBar?.visibility = View.GONE
            }
        }
        mainItem?.isClickable = false
        isSmsChecked = doBackupCheckBox?.isChecked ?: false
    }

    override suspend fun doInBackground(arg: Any?): Any? {

        writeLog("Starting reading SMS")

        val tmpList = ArrayList<SmsDataPacketKotlin>(0)
        var c = 0

        fun collectMessagesFromCursor(cursor: Cursor?): String {

            cursor?.let {cursorItem ->
                if (cursor.count > 0) cursorItem.moveToFirst()
                for (i in 0 until cursor.count){

                    if (doBackupCheckBox?.isChecked != true) {
                        writeLog("Break reading SMS")
                        break
                    }

                    smsTools.getSmsPacket(cursorItem, isSmsChecked).let {cdp ->
                        smsTools.errorEncountered.trim().let { err ->
                            if (err == "") {
                                tmpList.add(cdp)
                                publishProgress(c, "${getStringFromRes(R.string.reading_sms)}\n${c++}")
                                cursorItem.moveToNext()
                            } else return (err)
                        }
                    }
                }
            }
            return ""
        }


        try {

            writeLog("Reading inbox SMS")
            collectMessagesFromCursor(inboxCursor).let { if (it.trim() != "") throw Exception(it) }

            writeLog("Reading outbox SMS")
            collectMessagesFromCursor(outboxCursor).let { if (it.trim() != "") throw Exception(it) }

            writeLog("Reading sent SMS")
            collectMessagesFromCursor(sentCursor).let { if (it.trim() != "") throw Exception(it) }

            writeLog("Reading draft SMS")
            collectMessagesFromCursor(draftCursor).let { if (it.trim() != "") throw Exception(it) }

        } catch (e: Exception){
            error = e.message.toString()
            e.printStackTrace()
        }

        inboxCursor?.let {
            tryIt { it.close() }
        }
        outboxCursor?.let {
            tryIt { it.close() }
        }
        sentCursor?.let {
            tryIt { it.close() }
        }
        draftCursor?.let {
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