package balti.migrate.backupEngines

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import balti.filex.FileX
import balti.filex.FileXInit
import balti.migrate.AppInstance
import balti.migrate.AppInstance.Companion.CACHE_DIR
import balti.migrate.AppInstance.Companion.adbState
import balti.migrate.AppInstance.Companion.appPackets
import balti.migrate.AppInstance.Companion.callsList
import balti.migrate.AppInstance.Companion.contactsList
import balti.migrate.AppInstance.Companion.dpiText
import balti.migrate.AppInstance.Companion.fontScale
import balti.migrate.AppInstance.Companion.keyboardText
import balti.migrate.AppInstance.Companion.smsList
import balti.migrate.AppInstance.Companion.zipBatches
import balti.migrate.R
import balti.migrate.backupEngines.containers.ZipBatch
import balti.migrate.backupEngines.engines.*
import balti.migrate.backupEngines.utils.EngineJobResultHolder
import balti.migrate.simpleActivities.ProgressShowActivity_new
import balti.migrate.utilities.BackupProgressNotificationSystem
import balti.migrate.utilities.BackupProgressNotificationSystem.Companion.BackupUpdate
import balti.migrate.utilities.BackupProgressNotificationSystem.Companion.ProgressType.BACKUP_CANCELLED
import balti.migrate.utilities.BackupProgressNotificationSystem.Companion.ProgressType.PROGRESS_TYPE_FINISHED
import balti.migrate.utilities.CommonToolsKotlin
import balti.migrate.utilities.CommonToolsKotlin.Companion.ACTION_BACKUP_CANCEL
import balti.migrate.utilities.CommonToolsKotlin.Companion.CHANNEL_BACKUP_CANCELLING
import balti.migrate.utilities.CommonToolsKotlin.Companion.CHANNEL_BACKUP_END
import balti.migrate.utilities.CommonToolsKotlin.Companion.CHANNEL_BACKUP_RUNNING
import balti.migrate.utilities.CommonToolsKotlin.Companion.DEFAULT_INTERNAL_STORAGE_DIR
import balti.migrate.utilities.CommonToolsKotlin.Companion.ERR_BACKUP_SERVICE_ERROR
import balti.migrate.utilities.CommonToolsKotlin.Companion.ERR_GENERIC_BACKUP_ENGINE
import balti.migrate.utilities.CommonToolsKotlin.Companion.EXTRA_BACKUP_NAME
import balti.migrate.utilities.CommonToolsKotlin.Companion.EXTRA_CANONICAL_DESTINATION
import balti.migrate.utilities.CommonToolsKotlin.Companion.EXTRA_ERRORS
import balti.migrate.utilities.CommonToolsKotlin.Companion.EXTRA_FILEX_DESTINATION
import balti.migrate.utilities.CommonToolsKotlin.Companion.EXTRA_FINISHED_ZIP_PATHS
import balti.migrate.utilities.CommonToolsKotlin.Companion.EXTRA_FLASHER_ONLY
import balti.migrate.utilities.CommonToolsKotlin.Companion.EXTRA_IS_CANCELLED
import balti.migrate.utilities.CommonToolsKotlin.Companion.EXTRA_PROGRESS_ACTIVITY_FROM_FINISHED_NOTIFICATION
import balti.migrate.utilities.CommonToolsKotlin.Companion.EXTRA_TOTAL_TIME
import balti.migrate.utilities.CommonToolsKotlin.Companion.EXTRA_WARNINGS
import balti.migrate.utilities.CommonToolsKotlin.Companion.FILE_ERRORLOG
import balti.migrate.utilities.CommonToolsKotlin.Companion.FILE_PROGRESSLOG
import balti.migrate.utilities.CommonToolsKotlin.Companion.FLAG_UPDATE_CURRENT_PENDING_INTENT
import balti.migrate.utilities.CommonToolsKotlin.Companion.NOTIFICATION_ID_FINISHED
import balti.migrate.utilities.CommonToolsKotlin.Companion.NOTIFICATION_ID_ONGOING
import balti.migrate.utilities.CommonToolsKotlin.Companion.PREF_SYSTEM_CHECK
import balti.module.baltitoolbox.functions.FileHandlers.unpackAssetToInternal
import balti.module.baltitoolbox.functions.Misc.makeNotificationChannel
import balti.module.baltitoolbox.functions.Misc.tryIt
import balti.module.baltitoolbox.functions.SharedPrefs.getPrefBoolean
import kotlinx.coroutines.launch
import java.io.BufferedWriter
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

@SuppressLint("SimpleDateFormat")
class BackupServiceKotlin_new: LifecycleService() {

    companion object {

        /**
         * String holding path to backup suited for FileX object.
         * If SAF storage, this will be blank.
         * If traditional storage, this will contain the full path.
         */
        var fileXDestination: String = ""

        /**
         * Flag to denote if backup is marked as cancelled.
         * All backup engines check this flag before proceeding.
         */
        var cancelBackup: Boolean = false

        /**
         * Canonical destination is full path to backup location.
         * Usually never used.
         * [fileXDestination] is usually used everywhere.
         */
        var canonicalDestination: String = ""

        var backupName: String = ""
        var flasherOnly: Boolean = false

        /**
         * Will be set to true when the backup service starts.
         * Used to prevent backup from re-initiating.
         */
        private var backupStarted: Boolean = false

    }

    /**
     * Broadcast receiver to receive when "Cancel" Button is pressed from notification
     * or from [ProgressShowActivity_new].
     */
    private val cancelReceiver by lazy {
        object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                tryIt {
                    cancelBackup = true
                }
            }
        }
    }

    /**
     * Buffered writer to write to progressLog.txt
     */
    private var progressWriter: BufferedWriter? = null

    /**
     * Buffered writer to write to errorLog.txt
     */
    private var errorWriter: BufferedWriter? = null

    /**
     * Store last received title and subtask.
     * Do not write to log if same title and sub-task is received.
     */
    private var lastTitle = ""
    private var lastSubTask = ""

    private var startTime = 0L
    private var endTime = 0L

    private val timeStamp by lazy { SimpleDateFormat("yyyy.MM.dd_HH.mm.ss").format(Calendar.getInstance().time)}

    private val allErrors by lazy { ArrayList<String>(0) }
    private val allWarnings by lazy { ArrayList<String>(0) }

    private val zipCanonicalPaths by lazy { ArrayList<String>(0) }

    private val commonTools by lazy { CommonToolsKotlin(this) }

    /**
     * Path to busybox binary.
     * Busybox gets freshly unpacked from the assets and marked as executable.
     */
    private val busyboxBinaryPath by lazy {
        val cpuAbi = Build.SUPPORTED_ABIS[0]
        (if (cpuAbi == "x86" || cpuAbi == "x86_64")
            unpackAssetToInternal("busybox-x86", "busybox")
        else unpackAssetToInternal("busybox")).apply {
            tryIt { FileX.new(this, true).file.setExecutable(true) }
        }
    }

    override fun onCreate() {
        super.onCreate()

        /**
         * Service ongoing notification.
         */
        val loadingNotification = NotificationCompat.Builder(this, CHANNEL_BACKUP_RUNNING)
            .setContentTitle(getString(R.string.loading))
            .setSmallIcon(R.drawable.ic_notification_icon)
            .build()

        /**
         * Initiate log writers.
         */
        tryIt {
            progressWriter = BufferedWriter(OutputStreamWriter(FileX.new(CACHE_DIR, FILE_PROGRESSLOG, true).outputStream()))
            errorWriter = BufferedWriter(OutputStreamWriter(FileX.new(CACHE_DIR, FILE_ERRORLOG, true).outputStream()))
        }

        /**
         * Create notification channels.
         */
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            makeNotificationChannel(CHANNEL_BACKUP_RUNNING, CHANNEL_BACKUP_RUNNING, NotificationManager.IMPORTANCE_LOW)
            makeNotificationChannel(CHANNEL_BACKUP_END, CHANNEL_BACKUP_END, NotificationManager.IMPORTANCE_HIGH)
            makeNotificationChannel(CHANNEL_BACKUP_CANCELLING, CHANNEL_BACKUP_CANCELLING, NotificationManager.IMPORTANCE_MIN)
        }

        /** Register cancel receivers */
        registerReceiver(cancelReceiver, IntentFilter(ACTION_BACKUP_CANCEL))
        commonTools.LBM?.registerReceiver(cancelReceiver, IntentFilter(ACTION_BACKUP_CANCEL))

        startForeground(NOTIFICATION_ID_ONGOING, loadingNotification)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        if (intent == null) return super.onStartCommand(intent, flags, startId)

        if (!backupStarted) {
            canonicalDestination = intent.getStringExtra(EXTRA_CANONICAL_DESTINATION) ?: DEFAULT_INTERNAL_STORAGE_DIR
            val fxDestination = intent.getStringExtra(EXTRA_FILEX_DESTINATION) ?: ""
            backupName = intent.getStringExtra(EXTRA_BACKUP_NAME) ?: ""
            flasherOnly = intent.getBooleanExtra(EXTRA_FLASHER_ONLY, false)

            /**
             * If traditional storage, use full path. `fxDestination` will be same as [canonicalDestination].
             * Use [fileXDestination] = "fxDestination/[backupName]"
             *
             * For Storage access framework (SAF)
             * All generated backup files to be under a directory having name as backup name.
             * Use [fileXDestination] = [backupName]
             *
             * For Storage access framework (SAF)
             * If `fxDestination` is somehow not blank (should always be blank, but if in future, it is not blank)
             * then it means store backup in a different folder under root, not directly under granted root location.
             * Use [fileXDestination] = "fxDestination/[backupName]"
             */
            fileXDestination =
                when {
                    FileXInit.isTraditional -> "$fxDestination/$backupName"
                    fxDestination.isBlank() -> backupName
                    else -> "$fxDestination/$backupName"
                }

            startReceivingLogs()
            startBackup()
        }

        return super.onStartCommand(intent, flags, startId)
    }

    /**
     * Method to receive progress updates and record them in progressLog.txt
     */
    private fun startReceivingLogs(){
        lifecycleScope.launchWhenStarted {
            BackupProgressNotificationSystem.addListener(false){ update ->

                /**
                 * Write title if its a new title.
                 */
                if (update.title != lastTitle){
                    lastTitle = update.title
                    progressWriter?.write("\n======== $lastTitle ========\n")
                }

                /**
                 * Write subTask if its new.
                 */
                if (update.subTask != lastSubTask){
                    lastSubTask = update.subTask
                    progressWriter?.write("\n-------- $lastSubTask --------\n")
                }

                /**
                 * Write the log always.
                 */
                progressWriter?.write("${update.log}\n")
            }
        }
    }

    /**
     * Function to run all backup engines.
     * This is the heart of the backup service.
     * WIP
     */
    private fun startBackup(){
        lifecycleScope.launch {

            /**
             * Stores name of last engine that ran successfully.
             * Used for debugging purposes.
             */
            var lastSuccessEngine = ""

            try {
                backupStarted = true
                startTime = timeInMillis()
                zipCanonicalPaths.clear()
                allErrors.clear()
                allWarnings.clear()

                val listOfExtraFilesFromJobResults = ArrayList<FileX>(0)

                /**
                 * Function to collect all errors and warnings from job result.
                 */
                fun collectErrors(result: EngineJobResultHolder?) =
                    result?.run {
                        allErrors.addAll(errors)
                        allWarnings.addAll(warnings)
                    }

                /** STEP 1: System test */
                if (getPrefBoolean(PREF_SYSTEM_CHECK, true)) {
                    SystemTestingEngine(busyboxBinaryPath).executeWithResult()?.let {
                        collectErrors(it)
                        lastSuccessEngine = "SystemTestingEngine"
                    }
                }

                /**
                 * If errors are present after system test, end the backup.
                 */
                if (allErrors.isNotEmpty()) {
                    finishBackup(getString(R.string.backup_error_system_check_failed))
                    return@launch
                }

                /** STEP 2: Contacts backup */
                if (contactsList.isNotEmpty()) {
                    val contactsBackupName = "Contacts_$timeStamp.vcf"
                    ContactsBackupEngine(contactsBackupName).executeWithResult()?.let {
                        collectErrors(it)
                        if (it.success) {
                            listOfExtraFilesFromJobResults.add(it.result as FileX)
                        }
                        lastSuccessEngine = "ContactsBackupEngine"
                    }
                }

                /** STEP 3: SMS backup */
                if (smsList.isNotEmpty()) {
                    val smsBackupName = "Sms_$timeStamp.sms.db"
                    SmsBackupEngine(smsBackupName).executeWithResult()?.let {
                        collectErrors(it)
                        if (it.success) {
                            listOfExtraFilesFromJobResults.addAll(it.result as ArrayList<FileX>)
                        }
                        lastSuccessEngine = "SmsBackupEngine"
                    }
                }

                /** STEP 4: Calls backup */
                if (callsList.isNotEmpty()) {
                    val callsBackupName = "Calls_$timeStamp.calls.db"
                    CallsBackupEngine(callsBackupName).executeWithResult()?.let {
                        collectErrors(it)
                        if (it.success) {
                            listOfExtraFilesFromJobResults.addAll(it.result as ArrayList<FileX>)
                        }
                        lastSuccessEngine = "CallsBackupEngine"
                    }
                }

                /** STEP 5: Settings backup - ADB, Font scale, Keyboard, DPI */
                val isSettingsNull: Boolean = (dpiText == null && keyboardText == null && adbState == null && fontScale == null)
                if (!isSettingsNull) {
                    SettingsBackupEngine().executeWithResult()?.let {
                        collectErrors(it)
                        if (it.success) {
                            listOfExtraFilesFromJobResults.add(it.result as FileX)
                        }
                        lastSuccessEngine = "SettingsBackupEngine"
                    }
                }

                /**
                 * STEP 6: App backup
                 * Does not return any result.
                 */
                if (appPackets.isNotEmpty()) {
                    AppBackupEngine(busyboxBinaryPath).executeWithResult()?.let {
                        collectErrors(it)
                        lastSuccessEngine = "AppBackupEngine"
                    }
                }

                /**
                 * STEP 7: App backup verification
                 * Does not return any result.
                 */
                if (appPackets.isNotEmpty()) {
                    VerificationEngine(busyboxBinaryPath).executeWithResult()?.let {
                        collectErrors(it)
                        lastSuccessEngine = "VerificationEngine"
                    }
                }

                /**
                 * STEP 8: Making zip batches
                 * Result is ArrayList of [ZipBatch].
                 * If this step is not a success, then no point in proceeding further.
                 * In that case, end the backup and return.
                 */
                zipBatches.addAll(ArrayList(0))
                MakeZipBatch(listOfExtraFilesFromJobResults).executeWithResult()?.let {
                    collectErrors(it)
                    if (it.success) {
                        zipBatches.addAll(it.result as ArrayList<ZipBatch>)
                    } else {
                        finishBackup(getString(R.string.failed_to_make_batches))
                        return@launch
                    }
                    lastSuccessEngine = "MakeZipBatch"
                }

                /**
                 * For each zip batch, run the next steps individually.
                 */

                for ((i, zipBatch) in zipBatches.withIndex()) {

                    val partTag = "[${i + 1}/${zipBatches.size}]"
                    var result: EngineJobResultHolder? = null

                    /**
                     * Function collects errors from `result`.
                     * If result.success == `false`, returns `true` to indicate to `continue` the loop for next batch.
                     */
                    fun tryNextBatchDueToErrorsInThisBatch(currentStepName: String): Boolean {
                        collectErrors(result)
                        if (result != null) lastSuccessEngine = currentStepName
                        if (result?.success == false) {
                            allErrors.add("${ERR_BACKUP_SERVICE_ERROR}: ${getString(R.string.errors_in_batch)} - ${i + 1}")
                            return true
                        }
                        return false
                    }

                    /**
                     * STEP 9: Create updater script
                     * Does not return any result.
                     * If result.success is :
                     * - `true` - All good, proceed to next engine.
                     * - `false` - Errors in updater script. No point in proceeding in next engine. Try next zip batch.
                     */
                    result = UpdaterScriptMakerEngine(partTag, zipBatch, timeStamp).executeWithResult()
                    if (tryNextBatchDueToErrorsInThisBatch("UpdaterScriptMakerEngine $partTag")) {
                        continue
                    }

                    /**
                     * STEP 10: Compress the zip batch into a zip file.
                     * Result contains Pair(<FileX object for zip file>, <FileX object for fileList.txt>).
                     */
                    lateinit var fileListCopied: FileX
                    lateinit var lastZip: FileX
                    result = ZippingEngine(partTag, zipBatch).executeWithResult()
                    result?.run {
                        if (success) {
                            (this.result as Pair<FileX, FileX>).let {
                                lastZip = it.first
                                zipCanonicalPaths.add(it.first.canonicalPath)
                                fileListCopied = it.second
                            }
                        }
                    }
                    if (tryNextBatchDueToErrorsInThisBatch("ZippingEngine $partTag")) {
                        continue
                    }

                    /**
                     * STEP 11: Zip verification.
                     * Does not return any result.
                     *
                     * If the last engine fails, then the control will anyway jump to next batch.
                     * As such the control will not come to this statement.
                     * Hence, if `fileListCopied` and `lastZip` is not initialized (if !result.success in last engine)
                     * even then no error should occur.
                     */
                    result = ZipVerificationEngine(lastZip, partTag, flasherOnly, fileListCopied).executeWithResult()
                    if (tryNextBatchDueToErrorsInThisBatch("ZipVerificationEngine $partTag")) {
                        continue
                    }

                }
            }
            catch (e: Exception){
                e.printStackTrace()
                allErrors.add("$ERR_GENERIC_BACKUP_ENGINE: ${e.message.toString()}")
                allErrors.add("$ERR_GENERIC_BACKUP_ENGINE: Last success engine: $lastSuccessEngine")
                finishBackup("${getString(R.string.generic_backup_engine_error)}: ${e.message}")
            }

            finishBackup(isCancelled = cancelBackup)
        }
    }

    /**
     * Some finishing work to do after backup is complete.
     * Sends the final [BackupUpdate] broadcast and posts backup complete notification.
     * Both of these contain a Bundle containing:
     * [allErrors], [allWarnings], [zipCanonicalPaths], total time, boolean containing if backup is cancelled.
     * These are read in [ProgressShowActivity_new.updateUiOnBackupFinished].
     */
    private fun finishBackup(customTitle: String = "", isCancelled: Boolean = false){

        endTime = timeInMillis()

        /**
         * Write all errors.
         */
        errorWriter?.write("======== WARNINGS ========\n\n")
        allWarnings.forEach {
            errorWriter?.write("$it\n")
        }

        /**
         * Write all warnings.
         */
        errorWriter?.write("\n======== ERRORS ========\n\n")
        allErrors.forEach {
            errorWriter?.write("$it\n")
        }

        /**
         * End progressLog.txt and errorLog.txt with some additional information.
         */
        errorWriter?.write("\n--- Backup Name : $backupName ---\n")
        if (isCancelled) errorWriter?.write("--- Cancelled! ---\n")
        errorWriter?.write("--- Migrate version ${getString(R.string.current_version_name)} ---\n")
        progressWriter?.write("\n--- Backup Name : $backupName ---\n")
        progressWriter?.write("--- Total parts : ${zipCanonicalPaths.size} ---\n")
        progressWriter?.write("--- Migrate version ${getString(R.string.current_version_name)} ---\n")

        /**
         * Create variables for final [BackupUpdate] object.
         */
        val finishedTitle: String =
            when {
                customTitle.isNotBlank() -> customTitle
                isCancelled -> getString(R.string.backupCancelled)

                /**
                 * If errors are present, mention that.
                 */
                allErrors.isNotEmpty() -> getString(R.string.backupFinishedWithErrors)

                /**
                 * Next condition will run if the previous condition is false.
                 * i.e no errors, but warnings are present.
                 */
                allWarnings.isNotEmpty() -> getString(R.string.backupFinishedWithWarnings)

                /**
                 * If no condition satisfies, mention no errors.
                 */
                else -> getString(R.string.noErrors)
            }
        val finishedProgress = if(isCancelled) 0 else 100

        /**
         * Check the necessary extras in [BackupUpdate.extraInfoBundle].
         * Values are read in [ProgressShowActivity_new.updateUiOnBackupFinished].
         */
        val extraInfoBundle = Bundle().apply {
            putStringArrayList(EXTRA_ERRORS, allErrors)
            putStringArrayList(EXTRA_WARNINGS, allWarnings)
            putBoolean(EXTRA_IS_CANCELLED, isCancelled)
            putLong(EXTRA_TOTAL_TIME, (endTime-startTime))
            putStringArrayList(EXTRA_FINISHED_ZIP_PATHS, zipCanonicalPaths)
        }

        BackupProgressNotificationSystem.emitMessage(
            BackupUpdate(
                if (isCancelled) BACKUP_CANCELLED else PROGRESS_TYPE_FINISHED,
                finishedTitle,
                "",
                "",
                finishedProgress,
                extraInfoBundle = extraInfoBundle
            )
        )

        /**
         * Send the final notification. Use a different notification ID
         * to prevent the final notification from not be removed once the service is over.
         */
        val finalNotification = NotificationCompat.Builder(this, CHANNEL_BACKUP_END)
            .setContentTitle(finishedTitle)
            .setSmallIcon(R.drawable.ic_notification_icon)
            .setContentIntent(
                PendingIntent.getActivity(
                    this, CommonToolsKotlin.PENDING_INTENT_REQUEST_ID,
                    Intent(this, ProgressShowActivity_new::class.java).apply {
                        putExtra(EXTRA_PROGRESS_ACTIVITY_FROM_FINISHED_NOTIFICATION, true)
                    },
                    FLAG_UPDATE_CURRENT_PENDING_INTENT
                )
            )

        AppInstance.notificationManager.notify(NOTIFICATION_ID_FINISHED, finalNotification.build())

        clearAllDataForThisRun()

        BackupProgressNotificationSystem.resetImmediateUpdate()

        stopSelf()
    }

    /**
     * Clear all stored data.
     * This is done so that the variables in [AppInstance] are ready to receive new data for the next backup.
     */
    private fun clearAllDataForThisRun(){
        appPackets.clear()
        zipBatches.clear()
        contactsList.clear()
        callsList.clear()
        smsList.clear()
        dpiText = null
        keyboardText = null
        adbState = null
        fontScale = null
    }

    private fun timeInMillis() = Calendar.getInstance().timeInMillis

    override fun onDestroy() {
        super.onDestroy()

        backupStarted = false
        cancelBackup = false

        tryIt { commonTools.LBM?.unregisterReceiver(cancelReceiver) }
        tryIt { unregisterReceiver(cancelReceiver) }

        tryIt { progressWriter?.close() }
        tryIt { errorWriter?.close() }
    }
}