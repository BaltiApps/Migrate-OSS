package balti.migrate.backupEngines

import android.util.Log
import balti.filex.FileX
import balti.migrate.AppInstance
import balti.migrate.backupEngines.utils.EngineJobResultHolder
import balti.migrate.utilities.CommonToolsKotlin
import balti.migrate.utilities.CommonToolsKotlin.Companion.DEBUG_TAG
import balti.migrate.utilities.CommonToolsKotlin.Companion.DIR_APP_AUX_FILES
import balti.module.baltitoolbox.jobHandlers.AsyncCoroutineTask

abstract class ParentBackupClass_new: AsyncCoroutineTask(DISP_IO) {

    val fileXDestination: String?
        get() = BackupServiceKotlin_new.fileXDestination

    var cancelBackup: Boolean
        get() = BackupServiceKotlin_new.cancelBackup
        set(value) {
            BackupServiceKotlin_new.cancelBackup = value
        }

    val commonTools by lazy { CommonToolsKotlin() }

    val errors by lazy { ArrayList<String>(0) }
    val warnings by lazy { ArrayList<String>(0) }

    val rootLocation by lazy { FileX.new(fileXDestination?: "") }
    val appAuxFilesDir by lazy { FileX.new(AppInstance.appContext.filesDir.canonicalPath, DIR_APP_AUX_FILES, true) }
    val pathForAuxFiles by lazy { appAuxFilesDir.canonicalPath }
    val cacheFileX by lazy { FileX.new(AppInstance.CACHE_DIR, true) }

    final override suspend fun onPreExecute() {
        super.onPreExecute()
        if (!cancelBackup) preExecute()
    }

    final override suspend fun doInBackground(arg: Any?): Any {
        return backgroundProcessing()
    }

    final suspend fun executeWithResult(): EngineJobResultHolder? {
        return when {
            cancelBackup -> null
            fileXDestination == null -> {
                writeLog("Destination is null. Cancelling backup.")
                errors.add("Fatal error: Destination is null in $className. Cancelling backup.")
                cancelBackup = true
                EngineJobResultHolder(false, null, errors)
            }
            else -> super.executeWithResult(null) as EngineJobResultHolder
        }
    }

    final override suspend fun onPostExecute(result: Any?) {
        super.onPostExecute(result)
        if (!cancelBackup) postExecute(result)
    }

    fun writeLog(message: String){
        Log.d(DEBUG_TAG, "$className: $message")
    }

    abstract val className: String
    abstract fun preExecute()
    abstract suspend fun backgroundProcessing(): EngineJobResultHolder
    abstract fun postExecute(result: Any?)

}