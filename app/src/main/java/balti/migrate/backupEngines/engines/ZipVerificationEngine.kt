package balti.migrate.backupEngines.engines

import balti.migrate.AppInstance.Companion.MAX_WORKING_SIZE
import balti.migrate.R
import balti.migrate.backupEngines.BackupServiceKotlin
import balti.migrate.backupEngines.ParentBackupClass
import balti.migrate.backupEngines.containers.BackupIntentData
import balti.migrate.utilities.CommonToolKotlin.Companion.ERR_ZIP_ITEM_UNAVAILABLE
import balti.migrate.utilities.CommonToolKotlin.Companion.ERR_ZIP_TOO_BIG
import balti.migrate.utilities.CommonToolKotlin.Companion.ERR_ZIP_VERIFICATION_TRY_CATCH
import balti.migrate.utilities.CommonToolKotlin.Companion.EXTRA_PROGRESS_TYPE_ZIP_VERIFICATION
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

            resetBroadcast(true, title)

            val e = ZipFile(zipFile).entries()

            val fileSize = zipFile.length()/1024
            if (fileSize > MAX_WORKING_SIZE){
                verificationErrors.add("$ERR_ZIP_TOO_BIG${bd.errorTag}: ${commonTools.getHumanReadableStorageSpace(fileSize)}")
                return 0
            }

            var subTask = engineContext.getString(R.string.listing_zip_file)
            broadcastProgress(subTask, subTask, false)

            while (e.hasMoreElements()) {

                if (BackupServiceKotlin.cancelAll) break
                val entry = e.nextElement()
                contents.add(entry.name)
            }

            resetBroadcast(true, title)

            subTask = "${engineContext.getString(R.string.comparing_zip_contents)}(${zipList.size}/${contents.size})"
            broadcastProgress(subTask, subTask, false)

            for (i in 0 until zipList.size){

                val zipItem = zipList[i]
                if (BackupServiceKotlin.cancelAll) break

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