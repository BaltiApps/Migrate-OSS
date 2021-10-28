package balti.migrate.backupEngines.engines

import balti.filex.FileX
import balti.migrate.AppInstance.Companion.contactsList
import balti.migrate.R
import balti.migrate.backupEngines.BackupServiceKotlin
import balti.migrate.backupEngines.ParentBackupClass_new
import balti.migrate.utilities.BackupProgressNotificationSystem.Companion.ProgressType.PROGRESS_TYPE_CONTACTS
import balti.migrate.utilities.CommonToolsKotlin.Companion.ERR_CONTACTS_TRY_CATCH
import balti.module.baltitoolbox.functions.Misc.getPercentage

class ContactsBackupEngine(private val vcfFileName: String): ParentBackupClass_new(PROGRESS_TYPE_CONTACTS) {

    override val className: String = "ContactsBackupEngine"

    private val vcfFile by lazy { FileX.new(fileXDestination, vcfFileName) }
    private val contactPackets by lazy { contactsList }

    override suspend fun backgroundProcessing(): Any {

        try {

            FileX.new(fileXDestination).mkdirs()

            val title = getTitle(R.string.backing_contacts)

            resetBroadcast(false, title)

            heavyTask {

                vcfFile.createNewFile(overwriteIfExists = true)
                vcfFile.exists()

                vcfFile.startWriting(object : FileX.Writer(){
                    override fun writeLines() {
                        for (i in 0 until contactPackets.size) {

                            if (BackupServiceKotlin.cancelAll) break

                            val packet = contactPackets[i]

                            if (!packet.selected) continue

                            write("${packet.vcfData}\n")
                            broadcastProgress("", packet.fullName, true, getPercentage((i + 1), contactPackets.size))
                        }
                    }
                })
            }
        }
        catch (e: Exception){
            e.printStackTrace()
            errors.add("$ERR_CONTACTS_TRY_CATCH: ${e.message}")
        }

        return arrayListOf(vcfFile)
    }

}