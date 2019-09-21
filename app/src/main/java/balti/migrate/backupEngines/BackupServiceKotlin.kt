package balti.migrate.backupEngines

import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.AsyncTask.THREAD_POOL_EXECUTOR
import android.os.Build
import android.os.IBinder
import android.support.v4.app.NotificationCompat
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
import balti.migrate.utilities.CommonToolKotlin.Companion.BACKUP_NOTIFICATION_ID
import balti.migrate.utilities.CommonToolKotlin.Companion.CHANNEL_BACKUP_END
import balti.migrate.utilities.CommonToolKotlin.Companion.CHANNEL_BACKUP_RUNNING
import balti.migrate.utilities.CommonToolKotlin.Companion.CHANNEL_BACKUP_START
import balti.migrate.utilities.CommonToolKotlin.Companion.ERR_BACKUP_SERVICE_ERROR
import balti.migrate.utilities.CommonToolKotlin.Companion.EXTRA_APP_LOG
import balti.migrate.utilities.CommonToolKotlin.Companion.EXTRA_APP_NAME
import balti.migrate.utilities.CommonToolKotlin.Companion.EXTRA_ERRORS
import balti.migrate.utilities.CommonToolKotlin.Companion.EXTRA_PROGRESS_PERCENTAGE
import balti.migrate.utilities.CommonToolKotlin.Companion.EXTRA_PROGRESS_TYPE
import balti.migrate.utilities.CommonToolKotlin.Companion.EXTRA_PROGRESS_TYPE_APP_PROGRESS
import balti.migrate.utilities.CommonToolKotlin.Companion.EXTRA_PROGRESS_TYPE_CORRECTING
import balti.migrate.utilities.CommonToolKotlin.Companion.EXTRA_PROGRESS_TYPE_FINISHED
import balti.migrate.utilities.CommonToolKotlin.Companion.EXTRA_PROGRESS_TYPE_MAKING_APP_SCRIPTS
import balti.migrate.utilities.CommonToolKotlin.Companion.EXTRA_PROGRESS_TYPE_TESTING
import balti.migrate.utilities.CommonToolKotlin.Companion.EXTRA_PROGRESS_TYPE_VERIFYING
import balti.migrate.utilities.CommonToolKotlin.Companion.EXTRA_PROGRESS_TYPE_ZIP_PROGRESS
import balti.migrate.utilities.CommonToolKotlin.Companion.EXTRA_RETRY_LOG
import balti.migrate.utilities.CommonToolKotlin.Companion.EXTRA_SCRIPT_APP_NAME
import balti.migrate.utilities.CommonToolKotlin.Companion.EXTRA_TAR_CHECK_LOG
import balti.migrate.utilities.CommonToolKotlin.Companion.EXTRA_TEST_LOG
import balti.migrate.utilities.CommonToolKotlin.Companion.EXTRA_TITLE
import balti.migrate.utilities.CommonToolKotlin.Companion.EXTRA_TOTAL_TIME
import balti.migrate.utilities.CommonToolKotlin.Companion.EXTRA_ZIP_LOG
import balti.migrate.utilities.CommonToolKotlin.Companion.FILE_ERRORLOG
import balti.migrate.utilities.CommonToolKotlin.Companion.FILE_MAIN_PREF
import balti.migrate.utilities.CommonToolKotlin.Companion.FILE_PROGRESSLOG
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
import balti.migrate.utilities.CommonToolKotlin.Companion.MIGRATE_STATUS
import balti.migrate.utilities.CommonToolKotlin.Companion.PENDING_INTENT_BACKUP_CANCEL_ID
import balti.migrate.utilities.CommonToolKotlin.Companion.PENDING_INTENT_REQUEST_ID
import balti.migrate.utilities.CommonToolKotlin.Companion.PREF_COMPRESSION_LEVEL
import balti.migrate.utilities.CommonToolKotlin.Companion.PREF_DEFAULT_COMPRESSION_LEVEL
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
    }

    private val sharedPrefs by lazy { getSharedPreferences(FILE_MAIN_PREF, Context.MODE_PRIVATE) }

    override fun onBind(intent: Intent?): IBinder? = null

    private var cancelAll = false
    private var isBackupFinished = false
    private val commonTools by lazy { CommonToolKotlin(this) }

    private var progressWriter: BufferedWriter? = null
    private var errorWriter: BufferedWriter? = null
    private var lastTestLine = ""
    private var lastScriptAppName = ""
    private var lastAppLogLine = ""
    private var lastZipLine = ""
    private var lastVerifyLine = ""
    private var lastCorrectionLine = ""

    private var startedTest = false
    private var startedScript = false
    private var startedAppProgress = false
    private var startedZip = false
    private var startedVerify = false
    private var startedCorrection = false

    private val toReturnIntent by lazy { Intent(ACTION_BACKUP_PROGRESS) }
    private val allErrors by lazy { ArrayList<String>(0) }
    private val criticalErrors by lazy { ArrayList<String>(0) }

    private var lastErrorCount = 0

    private var TOTAL_TIME = 0L

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

    private val onGoingNotification by lazy {
        NotificationCompat.Builder(this, CHANNEL_BACKUP_START)
                .setContentTitle(getString(R.string.loading))
                .setSmallIcon(R.drawable.ic_notification_icon)
                .setContentIntent(
                        PendingIntent.getActivity(this, PENDING_INTENT_REQUEST_ID,
                        Intent(this, ProgressShowActivity::class.java).putExtras(toReturnIntent),
                        PendingIntent.FLAG_UPDATE_CURRENT)
                )
                .addAction(
                        NotificationCompat.Action(0, getString(android.R.string.cancel),
                                PendingIntent.getBroadcast(this, PENDING_INTENT_BACKUP_CANCEL_ID,
                                        Intent(ACTION_BACKUP_CANCEL), 0))
                )
                .setOngoing(true)
    }
    private val notificationManager by lazy { getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager }

    private val contactsBackupName = "Contacts_$timeStamp.vcf"
    private val smsBackupName = "Sms_$timeStamp.sms.db"
    private val callsBackupName = "Calls_$timeStamp.calls.db"

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
                    commonTools.tryIt { currentTask?.cancel(true) }
                    commonTools.dirDelete("$destination/$backupName")
                    backupFinished(getString(R.string.backupCancelled))
                }
            }
        }
    }

    private fun updateNotification(title: String, content: String = "", progressPercentage: Int = 0, indeterminate: Boolean, max: Int = 100){

        onGoingNotification.apply {
            setContentTitle(title)
            setContentText(content)
            setProgress(max, progressPercentage, indeterminate || isBackupFinished)
        }
        val notif = onGoingNotification.build()

        if (isBackupFinished) notif.actions = arrayOf()

        notificationManager.notify(BACKUP_NOTIFICATION_ID, notif)
    }

    private val progressReceiver by lazy {
        object : BroadcastReceiver(){
            override fun onReceive(context: Context?, intent: Intent?) {

                if (intent == null || !intent.hasExtra(EXTRA_PROGRESS_TYPE)) return

                toReturnIntent.putExtras(intent)

                var notificationContent = ""
                val isNotificationIndeterminate = intent.hasExtra(EXTRA_PROGRESS_PERCENTAGE)

                fun writeLogs(extraName : String) {
                    commonTools.tryIt {
                        if (intent.hasExtra(extraName)) {
                            intent.getStringExtra(extraName).run {

                                if (extraName == EXTRA_TEST_LOG && this != lastTestLine) {
                                    progressWriter?.write("$this\n")
                                    lastTestLine = this
                                } else if (extraName == EXTRA_SCRIPT_APP_NAME && this != lastScriptAppName) {
                                    progressWriter?.write("$this\n")
                                    lastScriptAppName = this
                                }  else if (extraName == EXTRA_APP_LOG && this != lastAppLogLine) {
                                    progressWriter?.write("$this\n")
                                    lastAppLogLine = this
                                } else if (extraName == EXTRA_ZIP_LOG && this != lastZipLine) {
                                    progressWriter?.write("$this\n")
                                    lastZipLine = this
                                } else if (extraName == EXTRA_APP_NAME || extraName == EXTRA_TAR_CHECK_LOG && this != lastVerifyLine) {
                                    progressWriter?.write("$this\n")
                                    lastVerifyLine = this
                                } else if (extraName == EXTRA_RETRY_LOG && this != lastCorrectionLine) {
                                    progressWriter?.write("$this\n")
                                    lastCorrectionLine = this
                                }

                                notificationContent =
                                        if (extraName == EXTRA_SCRIPT_APP_NAME || extraName == EXTRA_APP_NAME) this
                                        else ""
                            }
                        }
                    }
                }

                when (intent.getStringExtra(EXTRA_PROGRESS_TYPE)) {

                    EXTRA_PROGRESS_TYPE_TESTING -> {
                        if (!startedTest) {
                            progressWriter?.write("\n\n${MIGRATE_STATUS} System test logs\n")
                            startedTest = true
                        }
                        writeLogs(EXTRA_TEST_LOG)
                    }

                    EXTRA_PROGRESS_TYPE_MAKING_APP_SCRIPTS -> {
                        if (!startedScript) {
                            progressWriter?.write("\n\n${MIGRATE_STATUS} Making app backup scripts\n")
                            startedScript = true
                        }
                        writeLogs(EXTRA_SCRIPT_APP_NAME)
                    }

                    EXTRA_PROGRESS_TYPE_APP_PROGRESS -> {
                        if (!startedAppProgress) {
                            progressWriter?.write("\n\n${MIGRATE_STATUS} App backup logs\n")
                            startedAppProgress = true
                        }
                        writeLogs(EXTRA_APP_LOG)
                    }

                    EXTRA_PROGRESS_TYPE_ZIP_PROGRESS -> {
                        if (!startedZip) {
                            progressWriter?.write("\n\n${MIGRATE_STATUS} Zip logs\n")
                            startedZip = true
                        }
                        writeLogs(EXTRA_ZIP_LOG)
                    }

                    EXTRA_PROGRESS_TYPE_VERIFYING -> {
                        if (!startedVerify) {
                            progressWriter?.write("\n\n${MIGRATE_STATUS} App verification logs\n")
                            startedVerify = true
                        }
                        writeLogs(EXTRA_APP_NAME)
                        writeLogs(EXTRA_TAR_CHECK_LOG)
                    }

                    EXTRA_PROGRESS_TYPE_CORRECTING -> {
                        if (!startedCorrection) {
                            progressWriter?.write("\n\n${MIGRATE_STATUS} Correction logs\n")
                            startedCorrection = true
                        }
                        writeLogs(EXTRA_RETRY_LOG)
                    }

                }

                if (toReturnIntent.hasExtra(EXTRA_TITLE)){
                    updateNotification(toReturnIntent.getStringExtra(EXTRA_TITLE),
                            notificationContent, toReturnIntent.getIntExtra(EXTRA_PROGRESS_PERCENTAGE, -1),
                            isNotificationIndeterminate)
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

                currentBackupName = backupName
                currentDestination = destination

                if (dpiText != null || keyboardText != null || adbState != null || fontScale != null) isSettingsNull = false

                when (appBatches.size){
                    1 -> doFallThroughJob(JOBCODE_PEFORM_SYSTEM_TEST)
                    0 -> doFallThroughJob(JOBCODE_PEFORM_BACKUP_CONTACTS)
                    else -> doFallThroughJob(JOBCODE_PEFORM_SYSTEM_TEST)
                }
            }
        }
    }

    private fun getBackupIntentData(): BackupIntentData{
        if (appBatches.size > 1) {
            currentDestination = "$destination/$backupName"
            currentBackupName = commonTools.getMadePartName(currentPartNumber, appBatches.size)
        }
        return BackupIntentData(currentBackupName, currentDestination, currentPartNumber, appBatches.size)
    }

    override fun onCreate() {
        super.onCreate()

        serviceContext = this

        commonTools.tryIt {
            compressionLevel = sharedPrefs.getInt(PREF_COMPRESSION_LEVEL, PREF_DEFAULT_COMPRESSION_LEVEL)
        }

        commonTools.tryIt {
            progressWriter = BufferedWriter(FileWriter(File(externalCacheDir, FILE_PROGRESSLOG)))
            errorWriter = BufferedWriter(FileWriter(File(externalCacheDir, FILE_ERRORLOG)))
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            commonTools.makeNotificationChannel(CHANNEL_BACKUP_START, CHANNEL_BACKUP_START, NotificationManager.IMPORTANCE_DEFAULT)
            commonTools.makeNotificationChannel(CHANNEL_BACKUP_RUNNING, CHANNEL_BACKUP_RUNNING, NotificationManager.IMPORTANCE_DEFAULT)
            commonTools.makeNotificationChannel(CHANNEL_BACKUP_END, CHANNEL_BACKUP_END, NotificationManager.IMPORTANCE_DEFAULT)
        }

        commonTools.LBM?.registerReceiver(startBatchBackupReceiver, IntentFilter(ACTION_START_BATCH_BACKUP))
        commonTools.LBM?.registerReceiver(progressReceiver, IntentFilter(ACTION_BACKUP_PROGRESS))
        commonTools.LBM?.registerReceiver(cancelReceiver, IntentFilter(ACTION_BACKUP_CANCEL))
        registerReceiver(cancelReceiver, IntentFilter(ACTION_BACKUP_CANCEL))
        commonTools.LBM?.registerReceiver(requestProgressReceiver, IntentFilter(ACTION_REQUEST_BACKUP_DATA))

        startForeground(BACKUP_NOTIFICATION_ID, onGoingNotification.build())

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
                            JOBCODE_PEFORM_SYSTEM_TEST -> SystemTestingEngine(jCode, bd, busyboxBinaryPath)
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
        return if (currentPartNumber < appBatches.size) {
            commonTools.tryIt {
                progressWriter?.write("\n\n--- Next batch backup: ${currentPartNumber + 1} ---\n\n")
            }
            AppBackupEngine(currentAppBackupJobCode, bd, appBatches[currentPartNumber], doBackupInstallers, busyboxBinaryPath)
        } else null
    }

    private fun getUpdaterScriptTask(bd: BackupIntentData): UpdaterScriptMakerEngine?{

        try {

            val batch = when {
                appBatches.size == 0 -> AppBatch(ArrayList(0))
                else -> appBatches[currentPartNumber]
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
                if (jobSuccess) runConditionalTask(JOBCODE_PERFORM_ZIP_VERIFICATION)
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

        if (cancelAll){
            backupFinished(getString(R.string.backupCancelled))
            return
        }

        when (jobCode) {
            JOBCODE_PERFORM_APP_BACKUP_VERIFICATION -> try {
                currentAppVerificationJobCode = jobCode + currentPartNumber
                task = VerificationEngine(jobCode, bd, appBatches[currentPartNumber], busyboxBinaryPath)
            } catch (e: Exception) {
                e.printStackTrace()
                addError("$ERR_BACKUP_SERVICE_ERROR${bd.errorTag}: RUN_CONDITIONAL_TASK ${e.message}")

                // go to next job
                doFallThroughJob(JOBCODE_PERFORM_UPDATER_SCRIPT)
            }

            JOBCODE_PERFORM_ZIP_BACKUP -> try {
                currentZippingJobCode = jobCode + currentPartNumber
                task = ZippingEngine(jobCode, bd)
            } catch (e: Exception) {
                e.printStackTrace()
                addError("$ERR_BACKUP_SERVICE_ERROR${bd.errorTag}: RUN_CONDITIONAL_TASK ${e.message}")

                // no need to check for zip verification
                runNextBatch()
            }

            JOBCODE_PERFORM_ZIP_VERIFICATION -> try {
                currentZipVerificationJobCode = jobCode + currentPartNumber
                task = ZipVerificationEngine(jobCode, bd, zipListIfAny!!,
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
            backupFinished(getString(R.string.backupCancelled))
        }
        else {
            if (!isThisBatchSuccessful)
                addError("$ERR_BACKUP_SERVICE_ERROR[$currentPartNumber/${appBatches.size}]: " +
                        "${getString(R.string.errors_in_batch)} ${criticalErrors.size - lastErrorCount}")

            lastErrorCount = criticalErrors.size

            if (currentPartNumber + 1 < appBatches.size) {
                currentPartNumber++
                doFallThroughJob(JOBCODE_PERFORM_APP_BACKUP)
            } else backupFinished("")
        }
    }

    private fun backupFinished(errorTitle: String){

        isBackupFinished = true

        val title = when {
            errorTitle != "" -> errorTitle
            criticalErrors.size != 0 -> getString(R.string.backupFinishedWithErrors)
            else -> getString(R.string.noErrors)
        }

        toReturnIntent.apply {
            putExtra(EXTRA_PROGRESS_TYPE, EXTRA_PROGRESS_TYPE_FINISHED)
            putExtra(EXTRA_TITLE, title)
            putStringArrayListExtra(EXTRA_ERRORS, criticalErrors)
            putExtra(EXTRA_TOTAL_TIME, TOTAL_TIME)
        }
        commonTools.LBM?.sendBroadcast(toReturnIntent)

        for (e in allErrors){
            errorWriter?.write("$e\n")
        }
        commonTools.tryIt { errorWriter?.close() }

        progressWriter?.write("--- Total parts : " + appBatches.size + " ---\n")
        progressWriter?.write("--- Migrate version " + getString(R.string.current_version_name) + " ---\n")

        commonTools.tryIt { progressWriter?.close() }

        onGoingNotification.setOngoing(false)
        updateNotification(title, "", 0, false, 0)

        stopSelf()
    }

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