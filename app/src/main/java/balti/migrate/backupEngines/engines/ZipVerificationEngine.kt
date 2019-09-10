package balti.migrate.backupEngines.engines

import android.content.Context
import android.content.Intent
import android.os.AsyncTask
import balti.migrate.R
import balti.migrate.backupEngines.containers.BackupIntentData
import balti.migrate.backupEngines.utils.BackupDependencyComponent
import balti.migrate.backupEngines.utils.DaggerBackupDependencyComponent
import balti.migrate.backupEngines.utils.OnBackupComplete
import balti.migrate.utilities.CommonToolKotlin
import balti.migrate.utilities.CommonToolKotlin.Companion.ACTION_BACKUP_PROGRESS
import balti.migrate.utilities.CommonToolKotlin.Companion.ERR_ZIP_ITEM_UNAVAILABLE
import balti.migrate.utilities.CommonToolKotlin.Companion.ERR_ZIP_VERIFICATION_TRY_CATCH
import balti.migrate.utilities.CommonToolKotlin.Companion.EXTRA_BACKUP_NAME
import balti.migrate.utilities.CommonToolKotlin.Companion.EXTRA_MADE_PART_NAME
import balti.migrate.utilities.CommonToolKotlin.Companion.EXTRA_PART_NUMBER
import balti.migrate.utilities.CommonToolKotlin.Companion.EXTRA_PROGRESS_PERCENTAGE
import balti.migrate.utilities.CommonToolKotlin.Companion.EXTRA_PROGRESS_TYPE
import balti.migrate.utilities.CommonToolKotlin.Companion.EXTRA_PROGRESS_TYPE_ZIP_VERIFICATION
import balti.migrate.utilities.CommonToolKotlin.Companion.EXTRA_TITLE
import balti.migrate.utilities.CommonToolKotlin.Companion.EXTRA_TOTAL_PARTS
import balti.migrate.utilities.CommonToolKotlin.Companion.EXTRA_ZIP_VERIFICATION_LOG
import java.io.File
import java.util.zip.ZipFile
import javax.inject.Inject

class ZipVerificationEngine(private val jobcode: Int,
                            private val bd: BackupIntentData,
                            private val zipList: ArrayList<String>,
                            private val zipFile: File) : AsyncTask<Any, Any, Any>() {


    @Inject lateinit var engineContext: Context
    private var isBackupCancelled = false

    private val onBackupComplete by lazy { engineContext as OnBackupComplete }

    private val backupDependencyComponent: BackupDependencyComponent
            by lazy { DaggerBackupDependencyComponent.create() }

    private val commonTools by lazy { CommonToolKotlin(engineContext) }
    private val madePartName by lazy { commonTools.getMadePartName(bd) }

    private val verificationErrors by lazy { ArrayList<String>(0) }

    private val actualBroadcast by lazy {
        Intent(ACTION_BACKUP_PROGRESS).apply {
            putExtra(EXTRA_BACKUP_NAME, bd.backupName)
            putExtra(EXTRA_PROGRESS_TYPE, EXTRA_PROGRESS_TYPE_ZIP_VERIFICATION)
            putExtra(EXTRA_TOTAL_PARTS, bd.totalParts)
            putExtra(EXTRA_PART_NUMBER, bd.partNumber)
            putExtra(EXTRA_PROGRESS_PERCENTAGE, 0)
            putExtra(EXTRA_MADE_PART_NAME, madePartName)
        }
    }

    override fun onPreExecute() {
        super.onPreExecute()
        backupDependencyComponent.inject(this)
    }

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
            commonTools.LBM?.sendBroadcast(actualBroadcast)

            val e = ZipFile(zipFile).entries()
            while (e.hasMoreElements()) {

                if (isBackupCancelled) break
                val entry = e.nextElement()
                contents.add(entry.name)

                actualBroadcast.putExtra(EXTRA_ZIP_VERIFICATION_LOG, "listing: ${entry.name}")
                commonTools.LBM?.sendBroadcast(actualBroadcast)
            }

            actualBroadcast.apply {
                putExtra(EXTRA_ZIP_VERIFICATION_LOG, "\n\n${engineContext.getString(R.string.comparing_zip_contents)}")
                putExtra(EXTRA_PROGRESS_PERCENTAGE, 0)
            }
            commonTools.LBM?.sendBroadcast(actualBroadcast)

            for (i in 0 until zipList.size){

                val zipItem = zipList[i]
                if (isBackupCancelled) break

                actualBroadcast.apply {
                    putExtra(EXTRA_PROGRESS_PERCENTAGE, commonTools.getPercentage(i+1, zipList.size))
                    putExtra(EXTRA_ZIP_VERIFICATION_LOG, "checking: $zipItem")
                }
                commonTools.LBM?.sendBroadcast(actualBroadcast)

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

    override fun onPostExecute(result: Any?) {
        super.onPostExecute(result)
        if (verificationErrors.size == 0)
            onBackupComplete.onBackupComplete(jobcode, true, 0)
        else onBackupComplete.onBackupComplete(jobcode, false, verificationErrors)
    }

    override fun onCancelled() {
        super.onCancelled()
        isBackupCancelled = true
    }

}