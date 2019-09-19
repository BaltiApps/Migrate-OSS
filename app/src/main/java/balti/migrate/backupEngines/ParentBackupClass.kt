package balti.migrate.backupEngines

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.AsyncTask
import balti.migrate.backupEngines.containers.BackupIntentData
import balti.migrate.backupEngines.utils.BackupDependencyComponent
import balti.migrate.backupEngines.utils.DaggerBackupDependencyComponent
import balti.migrate.backupEngines.utils.OnBackupComplete
import balti.migrate.utilities.CommonToolKotlin
import balti.migrate.utilities.CommonToolKotlin.Companion.ACTION_BACKUP_PROGRESS
import balti.migrate.utilities.CommonToolKotlin.Companion.EXTRA_BACKUP_NAME
import balti.migrate.utilities.CommonToolKotlin.Companion.EXTRA_MADE_PART_NAME
import balti.migrate.utilities.CommonToolKotlin.Companion.EXTRA_PART_NUMBER
import balti.migrate.utilities.CommonToolKotlin.Companion.EXTRA_PROGRESS_PERCENTAGE
import balti.migrate.utilities.CommonToolKotlin.Companion.EXTRA_PROGRESS_TYPE
import balti.migrate.utilities.CommonToolKotlin.Companion.EXTRA_TOTAL_PARTS
import balti.migrate.utilities.CommonToolKotlin.Companion.FILE_FILE_LIST
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import javax.inject.Inject

abstract class ParentBackupClass(private val bd: BackupIntentData,
                                 private val intentType: String): AsyncTask<Any, Any, Any>() {


    @Inject lateinit var engineContext: Context
    @Inject lateinit var sharedPreferences: SharedPreferences

    var isBackupCancelled = false

    val onBackupComplete by lazy { engineContext as OnBackupComplete }

    private val backupDependencyComponent: BackupDependencyComponent
            by lazy { DaggerBackupDependencyComponent.create() }

    val commonTools by lazy { CommonToolKotlin(engineContext) }
    val madePartName by lazy { commonTools.getMadePartName(bd.partNumber, bd.totalParts) }
    val actualDestination by lazy { "${bd.destination}/${bd.backupName}" }

    var customCancelFunction: (() -> Unit)? = null
    var customPreExecuteFunction: (() -> Unit)? = null

    fun writeToFileList(fileName: String){
        BufferedWriter(FileWriter(File(actualDestination, FILE_FILE_LIST), true)).run {
            this.write("$fileName\n")
            this.close()
        }
    }

    val actualBroadcast by lazy {
        Intent(ACTION_BACKUP_PROGRESS).apply {
            putExtra(EXTRA_BACKUP_NAME, bd.backupName)
            putExtra(EXTRA_PROGRESS_TYPE, intentType)
            putExtra(EXTRA_TOTAL_PARTS, bd.totalParts)
            putExtra(EXTRA_PART_NUMBER, bd.partNumber)
            putExtra(EXTRA_PROGRESS_PERCENTAGE, 0)
            putExtra(EXTRA_MADE_PART_NAME, madePartName)
        }
    }

    override fun onPreExecute() {
        super.onPreExecute()
        backupDependencyComponent.masterInject(this)
        customPreExecuteFunction?.invoke()
    }

    override fun onCancelled() {
        super.onCancelled()
        isBackupCancelled = true
        customCancelFunction?.invoke()
    }
}