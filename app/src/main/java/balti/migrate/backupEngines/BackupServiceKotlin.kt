package balti.migrate.backupEngines

import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.AsyncTask
import android.os.AsyncTask.THREAD_POOL_EXECUTOR
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import balti.migrate.AppInstance
import balti.migrate.AppInstance.Companion.sharedPrefs
import balti.migrate.R
import balti.migrate.backupEngines.containers.BackupIntentData
import balti.migrate.backupEngines.engines.*
import balti.migrate.backupEngines.utils.OnBackupComplete
import balti.migrate.extraBackupsActivity.apps.containers.AppBatch
import balti.migrate.extraBackupsActivity.calls.containers.CallsDataPacketsKotlin
import balti.migrate.extraBackupsActivity.contacts.containers.ContactsDataPacketKotlin
import balti.migrate.extraBackupsActivity.sms.containers.SmsDataPacketKotlin
import balti.migrate.extraBackupsActivity.wifi.containers.WifiDataPacket
import balti.migrate.simpleActivities.ProgressShowActivity
import balti.migrate.utilities.CommonToolKotlin
import balti.migrate.utilities.CommonToolKotlin.Companion.ACTION_BACKUP_CANCEL
import balti.migrate.utilities.CommonToolKotlin.Companion.ACTION_BACKUP_PROGRESS
import balti.migrate.utilities.CommonToolKotlin.Companion.ACTION_BACKUP_SERVICE_STARTED
import balti.migrate.utilities.CommonToolKotlin.Companion.ACTION_REQUEST_BACKUP_DATA
import balti.migrate.utilities.CommonToolKotlin.Companion.ACTION_START_BATCH_BACKUP
import balti.migrate.utilities.CommonToolKotlin.Companion.ALL_SUPPRESSED_ERRORS
import balti.migrate.utilities.CommonToolKotlin.Companion.BACKUP_NAME_SETTINGS
import balti.migrate.utilities.CommonToolKotlin.Companion.CHANNEL_BACKUP_CANCELLING
import balti.migrate.utilities.CommonToolKotlin.Companion.CHANNEL_BACKUP_END
import balti.migrate.utilities.CommonToolKotlin.Companion.CHANNEL_BACKUP_RUNNING
import balti.migrate.utilities.CommonToolKotlin.Companion.ERR_BACKUP_SERVICE_ERROR
import balti.migrate.utilities.CommonToolKotlin.Companion.EXTRA_ERRORS
import balti.migrate.utilities.CommonToolKotlin.Companion.EXTRA_IS_CANCELLED
import balti.migrate.utilities.CommonToolKotlin.Companion.EXTRA_PROGRESS_PERCENTAGE
import balti.migrate.utilities.CommonToolKotlin.Companion.EXTRA_PROGRESS_TYPE
import balti.migrate.utilities.CommonToolKotlin.Companion.EXTRA_PROGRESS_TYPE_CALLS
import balti.migrate.utilities.CommonToolKotlin.Companion.EXTRA_PROGRESS_TYPE_CONTACTS
import balti.migrate.utilities.CommonToolKotlin.Companion.EXTRA_PROGRESS_TYPE_FINISHED
import balti.migrate.utilities.CommonToolKotlin.Companion.EXTRA_PROGRESS_TYPE_SMS
import balti.migrate.utilities.CommonToolKotlin.Companion.EXTRA_PROGRESS_TYPE_WAITING_TO_CANCEL
import balti.migrate.utilities.CommonToolKotlin.Companion.EXTRA_TASKLOG
import balti.migrate.utilities.CommonToolKotlin.Companion.EXTRA_TITLE
import balti.migrate.utilities.CommonToolKotlin.Companion.EXTRA_TOTAL_TIME
import balti.migrate.utilities.CommonToolKotlin.Companion.FILE_ERRORLOG
import balti.migrate.utilities.CommonToolKotlin.Companion.FILE_PROGRESSLOG
import balti.migrate.utilities.CommonToolKotlin.Companion.FILE_ZIP_NAME_EXTRAS
import balti.migrate.utilities.CommonToolKotlin.Companion.JOBCODE_PEFORM_BACKUP_CALLS
import balti.migrate.utilities.CommonToolKotlin.Companion.JOBCODE_PEFORM_BACKUP_CONTACTS
import balti.migrate.utilities.CommonToolKotlin.Companion.JOBCODE_PEFORM_BACKUP_SETTINGS
import balti.migrate.utilities.CommonToolKotlin.Companion.JOBCODE_PEFORM_BACKUP_SMS
import balti.migrate.utilities.CommonToolKotlin.Companion.JOBCODE_PEFORM_BACKUP_WIFI
import balti.migrate.utilities.CommonToolKotlin.Companion.JOBCODE_PEFORM_SYSTEM_TEST
import balti.migrate.utilities.CommonToolKotlin.Companion.JOBCODE_PERFORM_APP_BACKUP
import balti.migrate.utilities.CommonToolKotlin.Companion.JOBCODE_PERFORM_APP_BACKUP_VERIFICATION
import balti.migrate.utilities.CommonToolKotlin.Companion.JOBCODE_PERFORM_UPDATER_SCRIPT
import balti.migrate.utilities.CommonToolKotlin.Companion.JOBCODE_PERFORM_ZIP_BACKUP
import balti.migrate.utilities.CommonToolKotlin.Companion.JOBCODE_PERFORM_ZIP_VERIFICATION
import balti.migrate.utilities.CommonToolKotlin.Companion.NOTIFICATION_ID_CANCELLING
import balti.migrate.utilities.CommonToolKotlin.Companion.NOTIFICATION_ID_FINISHED
import balti.migrate.utilities.CommonToolKotlin.Companion.NOTIFICATION_ID_ONGOING
import balti.migrate.utilities.CommonToolKotlin.Companion.PREF_COMPRESSION_LEVEL
import balti.migrate.utilities.CommonToolKotlin.Companion.PREF_DEFAULT_COMPRESSION_LEVEL
import balti.migrate.utilities.CommonToolKotlin.Companion.PREF_DELETE_ERROR_BACKUP
import balti.migrate.utilities.CommonToolKotlin.Companion.PREF_SEPARATE_EXTRAS_BACKUP
import balti.migrate.utilities.CommonToolKotlin.Companion.PREF_SYSTEM_CHECK
import balti.migrate.utilities.CommonToolKotlin.Companion.TIMEOUT_WAITING_TO_CANCEL_TASK
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList


class BackupServiceKotlin: Service(), OnBackupComplete {

    companion object {

        var destination = ""
        var backupName = ""
        var appBatches = ArrayList<AppBatch>(0)

        var contactsList : ArrayList<ContactsDataPacketKotlin>? = null
        var callsList : ArrayList<CallsDataPacketsKotlin>? = null
        var smsList : ArrayList<SmsDataPacketKotlin>? = null
        var dpiText : String? = null
        var keyboardText : String? = null
        var adbState : Int? = null
        var fontScale : Double? = null
        var wifiData : WifiDataPacket? = null

        var doBackupInstallers = false

        lateinit var serviceContext: Context
        private set

        var cancelAll = false
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private val commonTools by lazy { CommonToolKotlin(this) }

    private var progressWriter: BufferedWriter? = null
    private var errorWriter: BufferedWriter? = null

    private var lastTitle = ""
    private var lastLog = ""
    private var lastDeterminateProgress = 0

    private val allErrors by lazy { ArrayList<String>(0) }
    private val criticalErrors by lazy { ArrayList<String>(0) }

    private var lastErrorCount = 0

    private var startTime = 0L
    private var endTime = 0L

    private var compressionLevel = 0

    private var currentTask: ParentBackupClass? = null
    private var currentPartNumber = 0
    private var currentAppBackupJobCode = 0
    private var currentAppVerificationJobCode = 0
    private var currentUpdaterScriptJobCode = 0
    private var currentZippingJobCode = 0
    private var currentZipVerificationJobCode = 0

    private var isSettingsNull = true

    private var currentBackupName = ""
    private var currentDestination = ""

    private val timeStamp by lazy { SimpleDateFormat("yyyy.MM.dd_HH.mm.ss").format(Calendar.getInstance().time)}

    private val contactsBackupName = "Contacts_$timeStamp.vcf"
    private val smsBackupName = "Sms_$timeStamp.sms.db"
    private val callsBackupName = "Calls_$timeStamp.calls.db"

    private var workingAppBatches = ArrayList<AppBatch>(0)

    private val toReturnIntent by lazy { Intent(ACTION_BACKUP_PROGRESS) }

    private val cancellingNotification by lazy {
        NotificationCompat.Builder(this, CHANNEL_BACKUP_CANCELLING)
            .setContentTitle(getString(R.string.cancelling))
            .setSmallIcon(R.drawable.ic_notification_icon)
                .build()
    }

    private fun addError(error: String, addToCriticalWithoutChecking: Boolean = true){
        allErrors.add(error)
        if (!addToCriticalWithoutChecking) {
            var isCritical = true
            for (it in ALL_SUPPRESSED_ERRORS) {
                if (error.startsWith(it)) {
                    isCritical = false
                    break
                }
            }
            if (isCritical) criticalErrors.add(error)
        }
        else criticalErrors.add(error)
    }

    private fun addError(errors: ArrayList<String>, addToCriticalWithoutChecking: Boolean = true){
        errors.forEach {
            addError(it, addToCriticalWithoutChecking)
        }
    }

    private val busyboxBinaryPath by lazy {
        val cpu_abi = Build.SUPPORTED_ABIS[0]
        if (cpu_abi == "x86" || cpu_abi == "x86_64")
            commonTools.unpackAssetToInternal("busybox-x86", "busybox", true)
        else commonTools.unpackAssetToInternal("busybox", "busybox", true)
    }

    private val cancelReceiver by lazy {
        object : BroadcastReceiver(){
            override fun onReceive(context: Context?, intent: Intent?) {
                commonTools.tryIt {

                    cancelAll = true

                    commonTools.LBM?.sendBroadcast(Intent(ACTION_BACKUP_PROGRESS)
                            .apply {
                                putExtra(EXTRA_PROGRESS_TYPE, EXTRA_PROGRESS_TYPE_WAITING_TO_CANCEL)
                                putExtra(EXTRA_TITLE, getString(R.string.cancelling))
                            }
                    )
                    AppInstance.notificationManager.notify(NOTIFICATION_ID_CANCELLING, cancellingNotification)

                    commonTools.doBackgroundTask({

                        currentTask?.let {
                            while (it.status != AsyncTask.Status.FINISHED) {
                                commonTools.tryIt { Thread.sleep(100) }
                            }
                            commonTools.tryIt { Thread.sleep(TIMEOUT_WAITING_TO_CANCEL_TASK) }
                        }

                    }, {
                        backupFinished(getString(R.string.backupCancelled))
                    })
                }
            }
        }
    }

    private val progressReceiver by lazy {
        object : BroadcastReceiver(){
            override fun onReceive(context: Context?, intent: Intent?) {

                if (intent == null || !intent.hasExtra(EXTRA_PROGRESS_TYPE) || !intent.hasExtra(EXTRA_TITLE)) return

                toReturnIntent.putExtras(intent)

                val type = intent.getStringExtra(EXTRA_PROGRESS_TYPE)
                if (type in arrayOf(EXTRA_PROGRESS_TYPE_CONTACTS, EXTRA_PROGRESS_TYPE_SMS, EXTRA_PROGRESS_TYPE_CALLS))
                    return

                intent.getStringExtra(EXTRA_TITLE).trim().run {
                    if (this != lastTitle) {
                        progressWriter?.write("\n$this\n")
                        lastTitle = this
                    }
                }

                if (intent.hasExtra(EXTRA_TASKLOG)){
                    intent.getStringExtra(EXTRA_TASKLOG).run {
                        if (this != lastLog) {
                            progressWriter?.write("$this\n")
                            lastLog = this
                        }
                    }
                }

                intent.getIntExtra(EXTRA_PROGRESS_PERCENTAGE, -1).run {
                    if (this != -1) lastDeterminateProgress = this
                }

            }
        }
    }

    private val requestProgressReceiver by lazy {
        object : BroadcastReceiver(){
            override fun onReceive(context: Context?, intent: Intent?) {
                toReturnIntent.action = ACTION_BACKUP_PROGRESS
                commonTools.LBM?.sendBroadcast(toReturnIntent)
            }
        }
    }

    private val startBatchBackupReceiver by lazy {
        object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {

                startTime = timeInMillis()

                currentBackupName = backupName
                currentDestination = destination

                cancelAll = false

                AppInstance.notificationManager.cancelAll()

                var isExtrasBackup = true

                if (dpiText != null || keyboardText != null || adbState != null || fontScale != null) isSettingsNull = false
                if (!isSettingsNull || contactsList != null || callsList != null || smsList != null || wifiData != null){
                    isExtrasBackup = true
                }

                appBatches.run {
                    if (!sharedPrefs.getBoolean(PREF_SEPARATE_EXTRAS_BACKUP, true)) workingAppBatches = this
                    else if (this.size <= 1) workingAppBatches = this
                    else if (!isExtrasBackup) workingAppBatches = this

                    when (this.size) {
                        1 -> doFallThroughJob(JOBCODE_PEFORM_SYSTEM_TEST)
                        0 -> doFallThroughJob(JOBCODE_PEFORM_BACKUP_CONTACTS)
                        else -> doFallThroughJob(JOBCODE_PEFORM_SYSTEM_TEST)
                    }
                }
            }
        }
    }

    private fun getBackupIntentData(): BackupIntentData{
        appBatches.size.run {
            if (this > 1) {
                currentDestination = "$destination/$backupName"
                currentBackupName =
                        if (workingAppBatches.size > 0) commonTools.getMadePartName(currentPartNumber, this)
                        else FILE_ZIP_NAME_EXTRAS
            }
            return BackupIntentData(currentBackupName, currentDestination, currentPartNumber, workingAppBatches.size)
        }
    }

    override fun onCreate() {
        super.onCreate()

        serviceContext = this

        val loadingNotification = NotificationCompat.Builder(this, CHANNEL_BACKUP_RUNNING)
                .setContentTitle(getString(R.string.loading))
                .setSmallIcon(R.drawable.ic_notification_icon)
                .build()

        commonTools.tryIt {
            compressionLevel = sharedPrefs.getInt(PREF_COMPRESSION_LEVEL, PREF_DEFAULT_COMPRESSION_LEVEL)
        }

        commonTools.tryIt {
            progressWriter = BufferedWriter(FileWriter(File(externalCacheDir, FILE_PROGRESSLOG)))
            errorWriter = BufferedWriter(FileWriter(File(externalCacheDir, FILE_ERRORLOG)))
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            commonTools.makeNotificationChannel(CHANNEL_BACKUP_RUNNING, CHANNEL_BACKUP_RUNNING, NotificationManager.IMPORTANCE_LOW)
            commonTools.makeNotificationChannel(CHANNEL_BACKUP_END, CHANNEL_BACKUP_END, NotificationManager.IMPORTANCE_HIGH)
            commonTools.makeNotificationChannel(CHANNEL_BACKUP_CANCELLING, CHANNEL_BACKUP_CANCELLING, NotificationManager.IMPORTANCE_MIN)
        }

        commonTools.LBM?.registerReceiver(startBatchBackupReceiver, IntentFilter(ACTION_START_BATCH_BACKUP))
        commonTools.LBM?.registerReceiver(progressReceiver, IntentFilter(ACTION_BACKUP_PROGRESS))
        commonTools.LBM?.registerReceiver(cancelReceiver, IntentFilter(ACTION_BACKUP_CANCEL))
        registerReceiver(cancelReceiver, IntentFilter(ACTION_BACKUP_CANCEL))
        commonTools.LBM?.registerReceiver(requestProgressReceiver, IntentFilter(ACTION_REQUEST_BACKUP_DATA))

        startForeground(NOTIFICATION_ID_ONGOING, loadingNotification)

        commonTools.LBM?.sendBroadcast(Intent(ACTION_BACKUP_SERVICE_STARTED))

    }

    private fun doFallThroughJob(jobCode: Int){

        var fallThrough = false
        val bd = getBackupIntentData()

        fun doJob(jCode: Int, workingObject: Any?){

            if (!cancelAll && (fallThrough || jobCode == jCode)) {

                fallThrough = true
                workingObject?.let {

                    currentTask = try {
                        when (jCode) {
                            JOBCODE_PEFORM_SYSTEM_TEST -> if (sharedPrefs.getBoolean(PREF_SYSTEM_CHECK, true)) SystemTestingEngine(jCode, bd, busyboxBinaryPath) else null
                            JOBCODE_PEFORM_BACKUP_CONTACTS -> ContactsBackupEngine(jCode, bd, workingObject as ArrayList<ContactsDataPacketKotlin>, contactsBackupName)
                            JOBCODE_PEFORM_BACKUP_SMS -> SmsBackupEngine(jCode, bd, workingObject as ArrayList<SmsDataPacketKotlin>, smsBackupName)
                            JOBCODE_PEFORM_BACKUP_CALLS -> CallsBackupEngine(jCode, bd, workingObject as ArrayList<CallsDataPacketsKotlin>, callsBackupName)
                            JOBCODE_PEFORM_BACKUP_WIFI -> WifiBackupEngine(jCode, bd, workingObject as WifiDataPacket)
                            JOBCODE_PEFORM_BACKUP_SETTINGS -> SettingsBackupEngine(jCode, bd, dpiText, adbState, fontScale, keyboardText)
                            JOBCODE_PERFORM_APP_BACKUP -> {
                                currentAppBackupJobCode = jCode + currentPartNumber
                                getAppBatchBackupTask(bd)
                            }
                            JOBCODE_PERFORM_UPDATER_SCRIPT -> {
                                currentUpdaterScriptJobCode = jCode + currentPartNumber
                                getUpdaterScriptTask(bd).apply {
                                    if (this == null) {
                                        runNextBatch()
                                    }
                                }
                            }
                            else -> null
                        }
                    } catch (e: Exception){
                        e.printStackTrace()
                        addError("$ERR_BACKUP_SERVICE_ERROR${bd.errorTag}: DO_JOB ${e.message}")
                        null
                    }

                    fallThrough = currentTask == null
                    currentTask?.executeOnExecutor(THREAD_POOL_EXECUTOR)
                }
            }
        }

        doJob(JOBCODE_PEFORM_SYSTEM_TEST, Any())
        doJob(JOBCODE_PEFORM_BACKUP_CONTACTS, contactsList)
        doJob(JOBCODE_PEFORM_BACKUP_SMS, smsList)
        doJob(JOBCODE_PEFORM_BACKUP_CALLS, callsList)
        doJob(JOBCODE_PEFORM_BACKUP_WIFI, wifiData)
        doJob(JOBCODE_PEFORM_BACKUP_SETTINGS, if (isSettingsNull) null else Any())

        doJob(JOBCODE_PERFORM_APP_BACKUP, Any())

        // if app backup works, app verification will be triggered from callback
        // if no app to backup, fall through to updater script engine

        doJob(JOBCODE_PERFORM_UPDATER_SCRIPT, Any())

        // fall through logic ends here. All further engines to be manually called from callback
    }

    private fun getAppBatchBackupTask(bd: BackupIntentData): AppBackupEngine?{
        return if (currentPartNumber < workingAppBatches.size) {
            commonTools.tryIt {
                progressWriter?.write("\n\n--- Next batch backup: ${currentPartNumber + 1} ---\n\n")
            }
            AppBackupEngine(currentAppBackupJobCode, bd, workingAppBatches[currentPartNumber], doBackupInstallers, busyboxBinaryPath)
        } else null
    }

    private fun getUpdaterScriptTask(bd: BackupIntentData): UpdaterScriptMakerEngine?{

        try {

            val batch = when {
                workingAppBatches.size == 0 -> AppBatch(ArrayList(0))
                else -> workingAppBatches[currentPartNumber]
            }

            return UpdaterScriptMakerEngine(currentUpdaterScriptJobCode, bd, batch, timeStamp,
                    if (contactsList != null) contactsBackupName else null,
                    if (smsList != null) smsBackupName else null,
                    if (callsList != null) callsBackupName else null,
                    if (!isSettingsNull) BACKUP_NAME_SETTINGS else null,
                    wifiData?.fileName)

        }
        catch (e: Exception){
            e.printStackTrace()
            addError("$ERR_BACKUP_SERVICE_ERROR${bd.errorTag}: UPDATER_SCRIPT ${e.message}")
            return null
        }
    }

    override fun onBackupComplete(jobCode: Int, jobSuccess: Boolean, jobResults: ArrayList<String>?) {

        when (jobCode){

            JOBCODE_PEFORM_SYSTEM_TEST -> {
                if (!jobSuccess) {
                    jobResults?.let { addError(it) }
                    backupFinished(getString(R.string.backup_error_system_check_failed))
                }
                else {
                    doFallThroughJob(JOBCODE_PEFORM_BACKUP_CONTACTS)
                }
            }

            JOBCODE_PEFORM_BACKUP_CONTACTS -> {
                if (!jobSuccess) jobResults?.let { addError(it) }
                doFallThroughJob(JOBCODE_PEFORM_BACKUP_SMS)
            }

            JOBCODE_PEFORM_BACKUP_SMS -> {
                if (!jobSuccess) jobResults?.let { addError(it) }
                doFallThroughJob(JOBCODE_PEFORM_BACKUP_CALLS)
            }

            JOBCODE_PEFORM_BACKUP_CALLS -> {
                if (!jobSuccess) jobResults?.let { addError(it) }
                doFallThroughJob(JOBCODE_PEFORM_BACKUP_WIFI)
            }

            JOBCODE_PEFORM_BACKUP_WIFI -> {
                if (!jobSuccess) jobResults?.let { addError(it) }
                doFallThroughJob(JOBCODE_PEFORM_BACKUP_SETTINGS)
            }

            JOBCODE_PEFORM_BACKUP_SETTINGS -> {
                if (!jobSuccess) jobResults?.let { addError(it) }
                doFallThroughJob(JOBCODE_PERFORM_APP_BACKUP)
            }

            currentAppBackupJobCode -> {
                jobResults?.let { addError(it, false) }
                runConditionalTask(JOBCODE_PERFORM_APP_BACKUP_VERIFICATION)
            }

            currentAppVerificationJobCode -> {
                jobResults?.let { addError(it, false) }
                doFallThroughJob(JOBCODE_PERFORM_UPDATER_SCRIPT)
            }

            currentUpdaterScriptJobCode -> {
                if (!jobSuccess) jobResults?.let { addError(it) }
                runConditionalTask(JOBCODE_PERFORM_ZIP_BACKUP)
            }

            currentZippingJobCode -> {
                if (jobSuccess) runConditionalTask(JOBCODE_PERFORM_ZIP_VERIFICATION, jobResults)
                else {
                    jobResults?.let { addError(it) }
                    runNextBatch()
                }
            }

            currentZipVerificationJobCode -> {
                if (!jobSuccess) jobResults?.let { addError(it) }
                runNextBatch(criticalErrors.size == lastErrorCount)
            }
        }
    }

    private fun runConditionalTask(jobCode: Int, zipListIfAny: ArrayList<String>? = null){

        val bd = getBackupIntentData()
        var task : ParentBackupClass? = null

        if (cancelAll) return

        when (jobCode) {

            JOBCODE_PERFORM_APP_BACKUP_VERIFICATION -> try {

                currentAppVerificationJobCode = jobCode + currentPartNumber
                task = VerificationEngine(currentAppVerificationJobCode, bd, workingAppBatches[currentPartNumber], busyboxBinaryPath)

            } catch (e: Exception) {

                e.printStackTrace()
                addError("$ERR_BACKUP_SERVICE_ERROR${bd.errorTag}: RUN_CONDITIONAL_TASK ${e.message}")

                // go to next job
                doFallThroughJob(JOBCODE_PERFORM_UPDATER_SCRIPT)
            }

            JOBCODE_PERFORM_ZIP_BACKUP -> try {

                currentZippingJobCode = jobCode + currentPartNumber
                task = ZippingEngine(currentZippingJobCode, bd)

            } catch (e: Exception) {

                e.printStackTrace()
                addError("$ERR_BACKUP_SERVICE_ERROR${bd.errorTag}: RUN_CONDITIONAL_TASK ${e.message}")

                // no need to check for zip verification
                runNextBatch()
            }

            JOBCODE_PERFORM_ZIP_VERIFICATION -> try {

                currentZipVerificationJobCode = jobCode + currentPartNumber
                task = ZipVerificationEngine(currentZipVerificationJobCode, bd, zipListIfAny!!,
                        File(currentDestination, "$currentBackupName.zip"))

            } catch (e: Exception) {

                e.printStackTrace()
                addError("$ERR_BACKUP_SERVICE_ERROR${bd.errorTag}: RUN_CONDITIONAL_TASK ${e.message}")
                runNextBatch()

            }
        }

        task?.run {
            task.executeOnExecutor(THREAD_POOL_EXECUTOR)
            currentTask = this
        }
    }

    private fun runNextBatch(isThisBatchSuccessful: Boolean = false){

        if (cancelAll){
            return
        }
        else {
            commonTools.dirDelete("$currentDestination/$currentBackupName")

            if (!isThisBatchSuccessful)
                addError("$ERR_BACKUP_SERVICE_ERROR[$currentPartNumber/${workingAppBatches.size}]: " +
                        "${getString(R.string.errors_in_batch)} ${criticalErrors.size - lastErrorCount}")

            lastErrorCount = criticalErrors.size

            if (workingAppBatches.size == 0 && appBatches.size != 0){
                workingAppBatches = appBatches
                currentPartNumber -= 1
            }

            if (currentPartNumber + 1 < workingAppBatches.size) {
                currentPartNumber++
                doFallThroughJob(JOBCODE_PERFORM_APP_BACKUP)
            } else backupFinished("")
        }
    }

    private fun backupFinished(errorTitle: String){

        val title = when {
            errorTitle != "" -> errorTitle
            criticalErrors.size != 0 -> getString(R.string.backupFinishedWithErrors)
            else -> getString(R.string.noErrors)
        }

        try {
            if (allErrors.size == 0) {
                errorWriter?.write("--- No errors! ---\n")
            } else for (e in allErrors) {
                errorWriter?.write("$e\n")
            }
            errorWriter?.write("\n--- Backup Name : $backupName ---\n")
            if (cancelAll) errorWriter?.write("--- Cancelled! ---\n")
            errorWriter?.write("--- Migrate version ${getString(R.string.current_version_name)} ---\n")

            progressWriter?.write("\n--- Backup Name : $backupName ---\n")
            progressWriter?.write("--- Total parts : ${workingAppBatches.size} ---\n")
            progressWriter?.write("--- Migrate version ${getString(R.string.current_version_name)} ---\n")
        }
        catch (e: Exception){
            e.printStackTrace()
        }

        endTime = timeInMillis()

        val returnIntent = Intent(ACTION_BACKUP_PROGRESS)
                .apply {
                    putExtra(EXTRA_PROGRESS_TYPE, EXTRA_PROGRESS_TYPE_FINISHED)
                    putExtra(EXTRA_TITLE, title)
                    putStringArrayListExtra(EXTRA_ERRORS, criticalErrors)
                    putExtra(EXTRA_IS_CANCELLED, cancelAll)
                    putExtra(EXTRA_TOTAL_TIME, endTime - startTime)
                    putExtra(EXTRA_PROGRESS_PERCENTAGE, if (criticalErrors.size == 0) 100 else lastDeterminateProgress)
                }

        commonTools.LBM?.sendBroadcast(returnIntent)

        AppInstance.notificationManager.cancel(NOTIFICATION_ID_CANCELLING)

        AppInstance.notificationManager.notify(NOTIFICATION_ID_FINISHED,
                NotificationCompat.Builder(this, CHANNEL_BACKUP_END)
                        .setContentTitle(title)
                        .setSmallIcon(R.drawable.ic_notification_icon)
                        .setContentIntent(
                                PendingIntent.getActivity(serviceContext, CommonToolKotlin.PENDING_INTENT_REQUEST_ID,
                                        Intent(this, ProgressShowActivity::class.java).putExtras(returnIntent),
                                        PendingIntent.FLAG_UPDATE_CURRENT))
                        .build())

        if ((errorTitle != "" || criticalErrors.size != 0) && sharedPrefs.getBoolean(PREF_DELETE_ERROR_BACKUP, true))
            commonTools.dirDelete("$destination/$backupName")

        stopSelf()
    }

    private fun timeInMillis() = Calendar.getInstance().timeInMillis

    override fun onDestroy() {
        super.onDestroy()
        commonTools.tryIt { commonTools.LBM?.unregisterReceiver(startBatchBackupReceiver) }
        commonTools.tryIt { commonTools.LBM?.unregisterReceiver(progressReceiver) }
        commonTools.tryIt { commonTools.LBM?.unregisterReceiver(cancelReceiver) }
        commonTools.tryIt { commonTools.LBM?.unregisterReceiver(requestProgressReceiver) }
        commonTools.tryIt { unregisterReceiver(cancelReceiver) }

        commonTools.tryIt { currentTask?.cancel(true) }

        commonTools.tryIt { progressWriter?.close() }
        commonTools.tryIt { errorWriter?.close() }
    }
}