package balti.migrate.backupEngines.engines

import balti.migrate.R
import balti.migrate.backupEngines.BackupServiceKotlin
import balti.migrate.backupEngines.ParentBackupClass
import balti.migrate.backupEngines.containers.BackupIntentData
import balti.migrate.extraBackupsActivity.contacts.containers.ContactsDataPacketKotlin
import balti.migrate.utilities.CommonToolKotlin.Companion.ERR_CONTACTS_TRY_CATCH
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

            val title = getTitle(R.string.backing_contacts)

            resetBroadcast(false, title)

            BufferedWriter(FileWriter(vcfFile, true)).run {

                for (i in 0 until contactPackets.size){

                    if (BackupServiceKotlin.cancelAll) break

                    val packet = contactPackets[i]

                    if (!packet.selected) continue

                    this.write("${packet.vcfData}\n")
                    broadcastProgress("", packet.fullName, true, commonTools.getPercentage((i+1), contactPackets.size))
                }

                this.close()
            }
        }
        catch (e: Exception){
            e.printStackTrace()
            errors.add("$ERR_CONTACTS_TRY_CATCH: ${e.message}")
        }

        return 0
    }

    override fun postExecuteFunction() {
        onEngineTaskComplete.onComplete(jobcode, errors, jobResults = vcfFile)
    }

}