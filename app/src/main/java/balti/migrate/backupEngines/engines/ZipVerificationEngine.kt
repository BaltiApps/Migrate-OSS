package balti.migrate.backupEngines.engines

import balti.migrate.AppInstance.Companion.MAX_WORKING_SIZE
import balti.migrate.R
import balti.migrate.backupEngines.BackupServiceKotlin
import balti.migrate.backupEngines.ParentBackupClass
import balti.migrate.backupEngines.containers.BackupIntentData
import balti.migrate.utilities.CommonToolKotlin.Companion.ERR_ZIP_ITEM_UNAVAILABLE
import balti.migrate.utilities.CommonToolKotlin.Companion.ERR_ZIP_TOO_BIG
import balti.migrate.utilities.CommonToolKotlin.Companion.ERR_ZIP_VERIFICATION_TRY_CATCH
import balti.migrate.utilities.CommonToolKotlin.Companion.EXTRA_PROGRESS_PERCENTAGE
import balti.migrate.utilities.CommonToolKotlin.Companion.EXTRA_PROGRESS_TYPE_ZIP_VERIFICATION
import balti.migrate.utilities.CommonToolKotlin.Companion.EXTRA_TITLE
import balti.migrate.utilities.CommonToolKotlin.Companion.EXTRA_ZIP_VERIFICATION_LOG
import java.io.File
import java.util.zip.ZipFile

class ZipVerificationEngine(private val jobcode: Int,
                            private val bd: BackupIntentData,
                            private val zipList: ArrayList<String>,
                            private val zipFile: File) : ParentBackupClass(bd, EXTRA_PROGRESS_TYPE_ZIP_VERIFICATION) {

    private val verificationErrors by lazy { ArrayList<String>(0) }

    override fun doInBackground(vararg params: Any?): Any {

        try {
            val contents = ArrayList<String>(0)

            val title = if (bd.totalParts > 1)
                engineContext.getString(R.string.verifying_zip) + " : " + madePartName
            else engineContext.getString(R.string.verifying_zip)

            actualBroadcast.apply {
                putExtra(EXTRA_TITLE, title)
                putExtra(EXTRA_ZIP_VERIFICATION_LOG, engineContext.getString(R.string.listing_zip_file))
                putExtra(EXTRA_PROGRESS_PERCENTAGE, 0)
            }
            broadcastProgress()

            val e = ZipFile(zipFile).entries()

            val fileSize = zipFile.length()
            if (fileSize > MAX_WORKING_SIZE){
                verificationErrors.add("$ERR_ZIP_TOO_BIG${bd.errorTag}: ${commonTools.getHumanReadableStorageSpace(fileSize)}")
                return 0
            }

            while (e.hasMoreElements()) {

                if (BackupServiceKotlin.cancelAll) break
                val entry = e.nextElement()
                contents.add(entry.name)

                actualBroadcast.putExtra(EXTRA_ZIP_VERIFICATION_LOG, "listing: ${entry.name}")
                broadcastProgress()
            }

            actualBroadcast.apply {
                putExtra(EXTRA_ZIP_VERIFICATION_LOG, "\n\n${engineContext.getString(R.string.comparing_zip_contents)}")
                putExtra(EXTRA_PROGRESS_PERCENTAGE, 0)
            }
            broadcastProgress()

            for (i in 0 until zipList.size){

                val zipItem = zipList[i]
                if (BackupServiceKotlin.cancelAll) break

                actualBroadcast.apply {
                    putExtra(EXTRA_PROGRESS_PERCENTAGE, commonTools.getPercentage(i+1, zipList.size))
                    putExtra(EXTRA_ZIP_VERIFICATION_LOG, "checking: $zipItem")
                }
                broadcastProgress()

                if (!contents.contains(zipItem)) {
                    verificationErrors.add("$ERR_ZIP_ITEM_UNAVAILABLE${bd.errorTag}: $i")
                }
            }

        }
        catch (e: Exception){
            e.printStackTrace()
            verificationErrors.add("$ERR_ZIP_VERIFICATION_TRY_CATCH${bd.errorTag}: ${e.message}")
        }

        return 0
    }

    override fun postExecuteFunction() {
        onBackupComplete.onBackupComplete(jobcode, verificationErrors.size == 0, verificationErrors)
    }

}