package balti.migrate.extraBackupsActivity.contacts

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
import balti.migrate.utilities.VcfToolsKotlin

class ReadContactsKotlin(private val jobCode: Int,
                         private val context: Context,
                         private val contactsMainItem: LinearLayout,
                         private val contactsSelectedStatus: TextView,
                         private val contactsReadProgressBar: ProgressBar,
                         private val doBackupContacts: CheckBox
                         ): AsyncTask<Any, Any, ArrayList<ContactsDataPacketKotlin>>() {

    private var contactsCount = 0
    private val vcfTools: VcfToolsKotlin by lazy { VcfToolsKotlin(context) }
    private val cursor: Cursor? by lazy { vcfTools.getContactsCursor() }
    private val vOp by lazy { ViewOperations(context) }
    private val onJobCompletion by lazy { context as OnJobCompletion }
    private var error = ""

    override fun onPreExecute() {
        super.onPreExecute()
        vOp.visibilitySet(contactsSelectedStatus, View.VISIBLE)
        vOp.visibilitySet(contactsReadProgressBar, View.VISIBLE)
        cursor?.let{
            contactsCount = it.count
            vOp.progressSet(contactsReadProgressBar, 0, contactsCount)
            vOp.enableSet(doBackupContacts, true)
        }
        if (cursor == null) {
            vOp.enableSet(doBackupContacts, false)
            vOp.checkSet(doBackupContacts, false)
            vOp.textSet(contactsSelectedStatus, R.string.reading_error)
            vOp.visibilitySet(contactsReadProgressBar, View.GONE)
        }
        vOp.clickableSet(contactsMainItem, false)
    }

    override fun doInBackground(vararg params: Any?): ArrayList<ContactsDataPacketKotlin> {
        val tmpList : ArrayList<ContactsDataPacketKotlin> = ArrayList(0)
        try {
            cursor?.let {
                it.moveToFirst()
                for (i in 0 until contactsCount){
                    val obj = ContactsDataPacketKotlin(vcfTools.getVcfData(it))
                    if (!tmpList.contains(obj))
                        tmpList.add(obj)
                    publishProgress(i, "${vOp.getStringFromRes(R.string.filtering_duplicates)}\n$i")
                    it.moveToNext()
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

    override fun onProgressUpdate(vararg values: Any?) {
        super.onProgressUpdate(*values)
        vOp.progressSet(contactsReadProgressBar, values[0] as Int)
        vOp.textSet(contactsSelectedStatus, values[1] as String)
    }

    override fun onPostExecute(result: ArrayList<ContactsDataPacketKotlin>?) {
        super.onPostExecute(result)
        if (error == "") onJobCompletion.onComplete(jobCode, true, result)
        else onJobCompletion.onComplete(jobCode, false, error)

        cursor?.let {
            try { it.close() } catch (_ : Exception){}
            vOp.clickableSet(contactsMainItem, contactsCount > 0)
        }
    }
}