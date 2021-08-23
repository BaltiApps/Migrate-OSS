package balti.migrate.extraBackupsActivity.engines.contacts

import android.content.Context
import android.database.Cursor
import android.view.View
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import balti.migrate.R
import balti.migrate.extraBackupsActivity.engines.contacts.containers.ContactsDataPacketKotlin
import balti.migrate.extraBackupsActivity.engines.contacts.utils.VcfToolsKotlin
import balti.migrate.extraBackupsActivity.utils.OnJobCompletion
import balti.migrate.extraBackupsActivity.utils.ViewOperations
import balti.module.baltitoolbox.jobHandlers.AsyncCoroutineTask

class ReadContactsKotlin_legacy(private val jobCode: Int,
                                private val context: Context,
                                private val menuMainItem: LinearLayout,
                                private val menuSelectedStatus: TextView,
                                private val menuReadProgressBar: ProgressBar,
                                private val doBackupCheckbox: CheckBox
                         ) : AsyncCoroutineTask() {

    private var contactsCount = 0
    private val vcfTools: VcfToolsKotlin by lazy { VcfToolsKotlin(context) }

    private val cursor: Cursor? by lazy { vcfTools.getContactsCursor() }

    private val vOp by lazy { ViewOperations(context) }
    private val onJobCompletion by lazy { context as OnJobCompletion }
    private var error = ""
    private var isContactsChecked = false

    override suspend fun onPreExecute() {
        super.onPreExecute()
        vOp.visibilitySet(menuSelectedStatus, View.VISIBLE)
        vOp.visibilitySet(menuReadProgressBar, View.VISIBLE)
        vOp.doSomething {
            cursor?.let {
                contactsCount = it.count
                vOp.progressSet(menuReadProgressBar, 0, contactsCount)
                vOp.enableSet(doBackupCheckbox, true)
            }
            if (cursor == null) {
                vOp.checkSet(doBackupCheckbox, false)
                vOp.textSet(menuSelectedStatus, R.string.reading_error)
                vOp.visibilitySet(menuReadProgressBar, View.GONE)
            }
        }
        vOp.clickableSet(menuMainItem, false)
        vOp.doSomething { isContactsChecked = doBackupCheckbox.isChecked }
    }

    override suspend fun doInBackground(arg: Any?): Any? {
        val tmpList : ArrayList<ContactsDataPacketKotlin> = ArrayList(0)
        try {
            cursor?.let {cursorItem ->
                if (contactsCount > 0) cursorItem.moveToFirst()
                for (i in 0 until contactsCount){
                    ContactsDataPacketKotlin(vcfTools.getVcfData(cursorItem), isContactsChecked).let { cdp ->
                        vcfTools.errorEncountered.trim().let {err ->
                            if (err == "") {
                                if (!tmpList.contains(cdp)) tmpList.add(cdp)
                                publishProgress(i, "${vOp.getStringFromRes(R.string.filtering_duplicates)}\n$i")
                                cursorItem.moveToNext()
                            }
                            else throw Exception(err)
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

    fun isDuplicate(cdp: ContactsDataPacketKotlin, dataPackets: ArrayList<ContactsDataPacketKotlin>): Boolean {
        for (dataPacket in dataPackets) {
            if (cdp.fullName == dataPacket.fullName && cdp.vcfData == dataPacket.vcfData)
                return true
        }
        return false
    }

    override suspend fun onProgressUpdate(vararg values: Any) {
        super.onProgressUpdate(*values)
        vOp.progressSet(menuReadProgressBar, values[0] as Int)
        vOp.textSet(menuSelectedStatus, values[1] as String)
    }

    override suspend fun onPostExecute(result: Any?) {
        super.onPostExecute(result)

        vOp.doSomething {

            if (error == "") {
                vOp.clickableSet(menuMainItem, contactsCount > 0)
                onJobCompletion.onComplete(jobCode, true, result)
            }
            else onJobCompletion.onComplete(jobCode, false, error)

            cursor?.let {
                vOp.doSomething { it.close() }
            }

        }
    }
}