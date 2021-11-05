package balti.migrate.backupEngines

import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.database.sqlite.SQLiteDatabase
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import balti.filex.FileX
import balti.migrate.AppInstance
import balti.migrate.R
import balti.migrate.backupEngines.engines.AppBackupEngine
import balti.migrate.backupEngines.utils.EngineJobResultHolder
import balti.migrate.simpleActivities.ProgressShowActivity_new
import balti.migrate.utilities.BackupProgressNotificationSystem
import balti.migrate.utilities.BackupProgressNotificationSystem.Companion.BackupUpdate
import balti.migrate.utilities.BackupProgressNotificationSystem.Companion.ProgressType
import balti.migrate.utilities.CommonToolsKotlin
import balti.migrate.utilities.CommonToolsKotlin.Companion.ACTION_BACKUP_CANCEL
import balti.migrate.utilities.CommonToolsKotlin.Companion.CHANNEL_BACKUP_RUNNING
import balti.migrate.utilities.CommonToolsKotlin.Companion.DEBUG_TAG
import balti.migrate.utilities.CommonToolsKotlin.Companion.DIR_APP_AUX_FILES
import balti.migrate.utilities.CommonToolsKotlin.Companion.NOTIFICATION_ID_ONGOING
import balti.migrate.utilities.CommonToolsKotlin.Companion.PENDING_INTENT_BACKUP_CANCEL_ID
import balti.migrate.utilities.CommonToolsKotlin.Companion.PENDING_INTENT_REQUEST_ID
import balti.module.baltitoolbox.functions.GetResources.getStringFromRes
import balti.module.baltitoolbox.functions.Misc.tryIt
import balti.module.baltitoolbox.jobHandlers.AsyncCoroutineTask
import java.io.BufferedWriter
import java.io.OutputStreamWriter

abstract class ParentBackupClass_new(
    defaultProgressType: ProgressType,
    private val checkBackupCancelled: Boolean = true
) : AsyncCoroutineTask(DISP_IO) {

    val fileXDestination: String get() = BackupServiceKotlin_new.fileXDestination
    val backupName: String get() = BackupServiceKotlin_new.backupName

    var cancelBackup: Boolean
        get() = if (checkBackupCancelled) BackupServiceKotlin_new.cancelBackup else false
        set(value) {
            if (checkBackupCancelled) BackupServiceKotlin_new.cancelBackup = value
        }

    val globalContext by lazy { AppInstance.appContext }

    val commonTools by lazy { CommonToolsKotlin() }
    val pm: PackageManager by lazy { globalContext.packageManager }

    val errors by lazy { ArrayList<String>(0) }
    val warnings by lazy { ArrayList<String>(0) }

    val rootLocation by lazy { FileX.new(fileXDestination) }
    val appAuxFilesDir by lazy { FileX.new(AppInstance.appContext.filesDir.canonicalPath, DIR_APP_AUX_FILES, true) }
    val pathForAuxFiles by lazy { appAuxFilesDir.canonicalPath }
    val CACHE by lazy { FileX.new(AppInstance.CACHE_DIR, true) }

    private var lastTitle = ""
    private var lastProgressPercent = 0
    private var isIndeterminate = true

    private val CancellingString: String by lazy { getStringFromRes(R.string.cancelling) }
    private val PleaseWaitString: String by lazy { getStringFromRes(R.string.please_wait) }

    /**
     * Progress type for this engine. This value may be changed from [resetBroadcast].
     * The value of this type is used to display different icons in [ProgressShowActivity_new].
     */
    private var engineProgressType: ProgressType = defaultProgressType

    /**
     * Notification to update the status.
     * Has a "Cancel" button that sends a broadcast [ACTION_BACKUP_CANCEL].
     */
    private val onGoingNotification by lazy {
        NotificationCompat.Builder(globalContext, CHANNEL_BACKUP_RUNNING)
                .setContentTitle(getStringFromRes(R.string.loading))
                .setSmallIcon(R.drawable.ic_notification_icon)
                .addAction(
                        NotificationCompat.Action(
                            0, getStringFromRes(android.R.string.cancel),
                            PendingIntent.getBroadcast(
                                globalContext, PENDING_INTENT_BACKUP_CANCEL_ID,
                                Intent(ACTION_BACKUP_CANCEL),
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
                            )
                        )
                )
    }

    /**
     * Return title of a backup step, appended with [partTag] if it is not blank.
     */
    fun getTitle(stringRes: Int): String =
        if (partTag == "") getStringFromRes(stringRes)
        else "${getStringFromRes(stringRes)} : ${getStringFromRes(R.string.part)} - $partTag"

    /**
     * Get an [SQLiteDatabase] instance from a file in storage.
     * [dataBaseFile] must be FileX traditional.
     */
    fun getDataBase(dataBaseFile: FileX): SQLiteDatabase {
        var dataBase: SQLiteDatabase = SQLiteDatabase.openOrCreateDatabase(dataBaseFile.canonicalPath, null)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1)
            dataBase = SQLiteDatabase.openDatabase(dataBaseFile.canonicalPath, null, SQLiteDatabase.NO_LOCALIZED_COLLATORS or SQLiteDatabase.OPEN_READWRITE)
        return dataBase
    }

    fun formatName(name: String): String {
        return name.replace(' ', '_')
            .replace('`', '\'')
            .replace('"', '\'')
    }

    /**
     * Method to update the [onGoingNotification].
     * Title of the notification is obtained from [lastTitle].
     * Content text is obtained from [subTask].
     * If [progressPercent] == -1, then indeterminate notification.
     */
    private fun updateNotification(subTask: String, progressPercent: Int){

        onGoingNotification.apply {
            setContentTitle(lastTitle)
            setContentText(subTask)
            setProgress(100, progressPercent, progressPercent == -1)
            setContentIntent(
                PendingIntent.getActivity(
                    globalContext, PENDING_INTENT_REQUEST_ID,
                    Intent(globalContext, ProgressShowActivity_new::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    },
                    (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0) or PendingIntent.FLAG_UPDATE_CURRENT
                )
            )
        }
        AppInstance.notificationManager.notify(NOTIFICATION_ID_ONGOING, onGoingNotification.build())
    }

    /**
     * Function to broadcast fine verbose log messages.
     * Also broadcasts subtask i.e subtitle.
     * Title is set once in [resetBroadcast].
     *
     * @param subTask String for subtitle.
     * Examples:
     * 1. Mention current app name that is being backed up.
     * 2. Mention different sub-steps in one process. Like in verifying app backups, different sub-steps
     * can be a. Verifying metadata files. b. Checking apk files. c. Verifying tar.gz files.
     * @param taskLog Verbose log messages. Example it shows the output of `tar` utility from root shell.
     * @param showNotification If `true` will cause [onGoingNotification] to be updated.
     * To be set to `false` if a step has too many updates in a very short time interval,
     * to prevent freezing system ui.
     * Also can be set to `false` for a step with indeterminate progress.
     * @param progressPercent Number denoting progress within 0-100.
     * If set to -1, then last received value of progress is used.
     */
    fun broadcastProgress(subTask: String, taskLog: String, showNotification: Boolean, progressPercent: Int = -1){

        //if (cancelBackup) return

        val progress = progressPercent.let {
            when {
                isIndeterminate -> -1
                it == -1 -> lastProgressPercent
                else -> it
            }
        }
        lastProgressPercent = progress

        val update: BackupUpdate = if (cancelBackup) {
            lastTitle = CancellingString
            isIndeterminate = true
            BackupUpdate(
                ProgressType.PROGRESS_TYPE_WAITING_TO_CANCEL,
                lastTitle,
                PleaseWaitString,
                taskLog.let { if (it == subTask) "" else it },
                -1, // Indeterminate progress
            )
        }
        else {
            BackupUpdate(
                engineProgressType,
                lastTitle,
                subTask,
                taskLog.let { if (it == subTask) "" else it },
                progress,
            )
        }

        BackupProgressNotificationSystem.emitMessage(update)

        if (showNotification || cancelBackup) {
            /**
             * If cancelBackup is `true`, update notification even if [showNotification] = `false`.
             * In all engines, if cancelBackup becomes `true`,
             * usually the engine terminates quickly without too many updates.
             * Hence this will, most likely, not freeze system UI by too many updates.
             */
            updateNotification(update.subTask, update.progressPercent)
        }
    }

    /**
     * Use this function at the beginning of a big backup step to reset the title.
     * Also sets the progress type.
     * Usually put at the start of all engine classes.
     * But an engine can also have multiple reset stages.
     * Example in [AppBackupEngine] - this method is called 3 times for different sub-steps -
     * during creating metadata, while actual backup, while moving metadata.
     *
     * @param isIndeterminateProgress If `true` then there is no numerical progress of the step.
     * Notification shows a continuous loading progress rather than a progress bar filling from 0 to 100.
     * This value is stored in [isIndeterminate] for the rest of the step.
     * Set to `true` for small steps which gets over quickly.
     * @param title String title for the rest of the backup step. Stored in [lastTitle].
     * @param newProgressType If an new step needs a separate value than the [engineProgressType].
     */
    fun resetBroadcast(isIndeterminateProgress: Boolean, title: String, newProgressType: ProgressType = engineProgressType){

        this.isIndeterminate = isIndeterminateProgress
        val progress = if (!isIndeterminateProgress) 0 else -1

        lastTitle = if (cancelBackup) CancellingString else title
        lastProgressPercent = progress
        engineProgressType = newProgressType

        val update: BackupUpdate = if (cancelBackup){
            BackupUpdate(
                ProgressType.PROGRESS_TYPE_WAITING_TO_CANCEL,
                lastTitle,
                PleaseWaitString,
                "",
                -1, // Indeterminate progress
            )
        }
        else {
            BackupUpdate(
                newProgressType,
                title,
                "",
                "",
                progress,
            )
        }

        BackupProgressNotificationSystem.emitMessage(update)

        updateNotification("", progress)
    }

    /**
     * Kill root sub-processes in side the parent root process.
     * @param suProcess The main parent root process.
     * @param pids Set of PIDs belonging to the sub-processes under [suProcess] which need to be terminated.
     */
    fun cancelTask(suProcess: Process?, vararg pids: Int) {

        tryIt {
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

            tryIt { killProcess.waitFor() }
            tryIt { suProcess?.waitFor() }
        }

    }

    final override suspend fun onPreExecute() {
        super.onPreExecute()
        if (!cancelBackup) preExecute()
    }

    final override suspend fun doInBackground(arg: Any?): Any? {
        return backgroundProcessing()
    }

    final suspend fun executeWithResult(): EngineJobResultHolder? {
        return when {
            cancelBackup -> null
            else -> {
                val result = super.executeWithResult(null)
                return EngineJobResultHolder(errors.isEmpty(), result, errors, warnings)
            }
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
    open fun preExecute() {}
    abstract suspend fun backgroundProcessing(): Any?
    open fun postExecute(result: Any?) {}

    /**
     * A string in the form of "[<current zip number>/<total zips>]".
     * Mostly added in front of errors.
     * Also used in [getTitle].
     *
     * MUST NOT be added in any file name or file path!
     */
    open val partTag: String = ""

}