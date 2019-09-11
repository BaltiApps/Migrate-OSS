package balti.migrate.backupEngines.engines

import balti.migrate.backupEngines.ParentBackupClass
import balti.migrate.backupEngines.containers.BackupIntentData
import balti.migrate.extraBackupsActivity.contacts.containers.ContactsDataPacketKotlin
import balti.migrate.utilities.CommonToolKotlin.Companion.ERR_CONTACTS_TRY_CATCH
import balti.migrate.utilities.CommonToolKotlin.Companion.EXTRA_CONTACT_NAME
import balti.migrate.utilities.CommonToolKotlin.Companion.EXTRA_PROGRESS_PERCENTAGE
import balti.migrate.utilities.CommonToolKotlin.Companion.EXTRA_PROGRESS_TYPE_CONTACTS
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter

class ContactsBackupEngine(private val jobcode: Int,
                           private val bd: BackupIntentData,
                           private val contactPackets: ArrayList<ContactsDataPacketKotlin>,
                           private val vcfFileName: String):
        ParentBackupClass(bd, EXTRA_PROGRESS_TYPE_CONTACTS) {

    private val vcfFile by lazy { File(actualDestination, vcfFileName) }
    private val errors by lazy { ArrayList<String>(0) }

    override fun doInBackground(vararg params: Any?): Any {

        try {

            File(actualDestination).mkdirs()
            if (vcfFile.exists()) vcfFile.delete()

            BufferedWriter(FileWriter(vcfFile, true)).let {bufferedWriter ->

                for (i in 0 until contactPackets.size){

                    val packet = contactPackets[i]
                    bufferedWriter.write("${packet.vcfData}\n")

                    actualBroadcast.apply {
                        putExtra(EXTRA_PROGRESS_PERCENTAGE, commonTools.getPercentage((i+1), contactPackets.size))
                        putExtra(EXTRA_CONTACT_NAME, packet.fullName)
                    }

                    commonTools.LBM?.sendBroadcast(actualBroadcast)
                }

            }
        }
        catch (e: Exception){
            e.printStackTrace()
            errors.add("$ERR_CONTACTS_TRY_CATCH${bd.errorTag}: ${e.message}")
        }

        return 0
    }

    override fun onPostExecute(result: Any?) {
        super.onPostExecute(result)
        if (errors.size == 0)
            onBackupComplete.onBackupComplete(jobcode, true, 0)
        else onBackupComplete.onBackupComplete(jobcode, false, errors)
    }

}