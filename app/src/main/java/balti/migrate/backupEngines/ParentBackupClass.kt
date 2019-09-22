package balti.migrate.backupEngines

import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.os.AsyncTask
import android.os.Build
import balti.migrate.AppInstance
import balti.migrate.backupEngines.containers.BackupIntentData
import balti.migrate.backupEngines.utils.OnBackupComplete
import balti.migrate.utilities.CommonToolKotlin
import balti.migrate.utilities.CommonToolKotlin.Companion.ACTION_BACKUP_PROGRESS
import balti.migrate.utilities.CommonToolKotlin.Companion.EXTRA_APP_NAME
import balti.migrate.utilities.CommonToolKotlin.Companion.EXTRA_BACKUP_NAME
import balti.migrate.utilities.CommonToolKotlin.Companion.EXTRA_MADE_PART_NAME
import balti.migrate.utilities.CommonToolKotlin.Companion.EXTRA_PART_NUMBER
import balti.migrate.utilities.CommonToolKotlin.Companion.EXTRA_PROGRESS_PERCENTAGE
import balti.migrate.utilities.CommonToolKotlin.Companion.EXTRA_PROGRESS_TYPE
import balti.migrate.utilities.CommonToolKotlin.Companion.EXTRA_SCRIPT_APP_NAME
import balti.migrate.utilities.CommonToolKotlin.Companion.EXTRA_TITLE
import balti.migrate.utilities.CommonToolKotlin.Companion.EXTRA_TOTAL_PARTS
import balti.migrate.utilities.CommonToolKotlin.Companion.FILE_FILE_LIST
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.OutputStreamWriter

abstract class ParentBackupClass(private val bd: BackupIntentData,
                                 private val intentType: String): AsyncTask<Any, Any, Any>() {

    val engineContext by lazy { BackupServiceKotlin.serviceContext }
    val sharedPreferences by lazy { AppInstance.sharedPrefs }

    val onBackupComplete by lazy { engineContext as OnBackupComplete }

    val commonTools by lazy { CommonToolKotlin(engineContext) }
    val madePartName by lazy { commonTools.getMadePartName(bd.partNumber, bd.totalParts) }
    val actualDestination by lazy { "${bd.destination}/${bd.backupName}" }

    var customPreExecuteFunction: (() -> Unit)? = null

    companion object {
        var isBackupStopped = false
        private set
    }

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

    fun broadcastProgress(){

        commonTools.LBM?.sendBroadcast(actualBroadcast)


        actualBroadcast.let {
            val notificationContent =
                    when {
                        it.hasExtra(EXTRA_SCRIPT_APP_NAME) -> it.getStringExtra(EXTRA_SCRIPT_APP_NAME)
                        it.hasExtra(EXTRA_APP_NAME) -> it.getStringExtra(EXTRA_APP_NAME)
                        else -> ""
                    }

            if (it.hasExtra(EXTRA_TITLE)) {

                BackupServiceKotlin.updateNotification(it.getStringExtra(EXTRA_TITLE),
                        notificationContent, it.getIntExtra(EXTRA_PROGRESS_PERCENTAGE, -1),
                        !it.hasExtra(EXTRA_PROGRESS_PERCENTAGE))

            }
        }
    }

    fun getDataBase(dataBaseFile: File): SQLiteDatabase{
        var dataBase: SQLiteDatabase = SQLiteDatabase.openOrCreateDatabase(dataBaseFile.absolutePath, null)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1)
            dataBase = SQLiteDatabase.openDatabase(dataBaseFile.absolutePath, null, SQLiteDatabase.NO_LOCALIZED_COLLATORS or SQLiteDatabase.OPEN_READWRITE)
        return dataBase
    }

    abstract fun postExecuteFunction()

    internal fun cancelTask(suProcess: Process?, vararg pids: Int) {

            commonTools.tryIt {
                val killProcess = Runtime.getRuntime().exec("su")

                val writer = BufferedWriter(OutputStreamWriter(killProcess.outputStream))
                fun killId(pid: Int) {
                    writer.write("kill -9 $pid\n")
                    writer.write("kill -15 $pid\n")
                }

                for (pid in pids)
                    if (pid != -999) killId(pid)

                writer.write("exit\n")
                writer.flush()

                commonTools.tryIt { killProcess.waitFor() }
                commonTools.tryIt { suProcess?.waitFor() }
            }

    }

    override fun onPreExecute() {
        super.onPreExecute()
        customPreExecuteFunction?.invoke()
    }

    override fun onPostExecute(result: Any?) {
        isBackupStopped = true
        postExecuteFunction()
        super.onPostExecute(result)
    }
}