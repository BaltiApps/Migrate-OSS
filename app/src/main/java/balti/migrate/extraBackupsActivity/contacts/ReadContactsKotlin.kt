package balti.migrate.extraBackupsActivity.contacts

import android.database.Cursor
import android.view.View
import balti.migrate.R
import balti.migrate.extraBackupsActivity.ParentReaderForExtras
import balti.migrate.extraBackupsActivity.ReaderJobResultHolder
import balti.migrate.extraBackupsActivity.contacts.containers.ContactsDataPacketKotlin
import balti.migrate.extraBackupsActivity.contacts.utils.VcfToolsKotlin
import balti.migrate.extraBackupsActivity.utils.ViewOperations
import balti.module.baltitoolbox.functions.Misc.tryIt

class ReadContactsKotlin(fragment: ContactsFragment): ParentReaderForExtras(fragment) {

    private var contactsCount = 0
    private val vcfTools: VcfToolsKotlin by lazy { VcfToolsKotlin(context) }

    private val cursor: Cursor? by lazy { vcfTools.getContactsCursor() }

    private val vOp by lazy { ViewOperations(context) }
    private var error = ""
    private var isContactsChecked = false

    override val className: String = "ReadContactsKotlin"

    override suspend fun onPreExecute() {
        super.onPreExecute()
        readStatusText?.visibility = View.VISIBLE
        readProgressBar?.visibility = View.VISIBLE
        tryIt {
            cursor?.let {
                contactsCount = it.count
                readProgressBar?.apply {
                    max = contactsCount
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
        isContactsChecked = doBackupCheckBox?.isChecked ?: false
    }

    override suspend fun doInBackground(arg: Any?): Any {

        writeLog("Starting reading")

        val tmpList: ArrayList<ContactsDataPacketKotlin> = ArrayList(0)
        try {
            cursor?.let { cursor ->
                if (contactsCount > 0) cursor.moveToFirst()
                for (i in 0 until contactsCount) {

                    if (doBackupCheckBox?.isChecked != true) {
                        writeLog("Break reading contacts")
                        break
                    }

                    ContactsDataPacketKotlin(vcfTools.getVcfData(cursor), isContactsChecked
                    ).let { cdp ->
                        vcfTools.errorEncountered.trim().let { err ->
                            if (err == "") {
                                if (!tmpList.contains(cdp)) tmpList.add(cdp)
                                publishProgress(
                                    i,
                                    "${vOp.getStringFromRes(R.string.filtering_duplicates)}\n$i"
                                )
                                cursor.moveToNext()
                            } else throw Exception(err)
                        }
                    }
                }
            }
        } catch (e: Exception) {
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

    fun isDuplicate(cdp: ContactsDataPacketKotlin, dataPackets: ArrayList<ContactsDataPacketKotlin>): Boolean {
        for (dataPacket in dataPackets) {
            if (cdp.fullName == dataPacket.fullName && cdp.vcfData == dataPacket.vcfData)
                return true
        }
        return false
    }

    override suspend fun onProgressUpdate(vararg values: Any) {
        super.onProgressUpdate(*values)
        readProgressBar?.progress = values[0] as Int
        readStatusText?.text = values[1] as String
    }
}