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
import balti.migrate.AppInstance.Companion.adbState
import balti.migrate.AppInstance.Companion.appPackets
import balti.migrate.AppInstance.Companion.callsList
import balti.migrate.AppInstance.Companion.contactsList
import balti.migrate.AppInstance.Companion.doBackupInstallers
import balti.migrate.AppInstance.Companion.dpiText
import balti.migrate.AppInstance.Companion.fontScale
import balti.migrate.AppInstance.Companion.keyboardText
import balti.migrate.AppInstance.Companion.sharedPrefs
import balti.migrate.AppInstance.Companion.smsList
import balti.migrate.AppInstance.Companion.wifiData
import balti.migrate.AppInstance.Companion.zipBatches
import balti.migrate.R
import balti.migrate.backupEngines.containers.BackupIntentData
import balti.migrate.backupEngines.containers.ZipAppBatch
import balti.migrate.backupEngines.engines.*
import balti.migrate.backupEngines.utils.OnEngineTaskComplete
import balti.migrate.extraBackupsActivity.apps.containers.AppPacket
import balti.migrate.extraBackupsActivity.calls.containers.CallsDataPacketsKotlin
import balti.migrate.extraBackupsActivity.contacts.containers.ContactsDataPacketKotlin
import balti.migrate.extraBackupsActivity.sms.containers.SmsDataPacketKotlin
import balti.migrate.extraBackupsActivity.wifi.containers.WifiDataPacket
import balti.migrate.simpleActivities.ProgressShowActivity
import balti.migrate.utilities.CommonToolKotlin
import balti.migrate.utilities.CommonToolKotlin.Companion.ACTION_BACKUP_CANCEL
import balti.migrate.utilities.CommonToolKotlin.Companion.ACTION_BACKUP_PROGRESS
import balti.migrate.utilities.CommonToolKotlin.Companion.ACTION_REQUEST_BACKUP_DATA
import balti.migrate.utilities.CommonToolKotlin.Companion.ALL_SUPPRESSED_ERRORS
import balti.migrate.utilities.CommonToolKotlin.Companion.CHANNEL_BACKUP_CANCELLING
import balti.migrate.utilities.CommonToolKotlin.Companion.CHANNEL_BACKUP_END
import balti.migrate.utilities.CommonToolKotlin.Companion.CHANNEL_BACKUP_RUNNING
import balti.migrate.utilities.CommonToolKotlin.Companion.ERR_BACKUP_SERVICE_ERROR
import balti.migrate.utilities.CommonToolKotlin.Companion.ERR_CONDITIONAL_TASK
import balti.migrate.utilities.CommonToolKotlin.Companion.EXTRA_BACKUP_NAME
import balti.migrate.utilities.CommonToolKotlin.Companion.EXTRA_DESTINATION
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
import balti.migrate.utilities.CommonToolKotlin.Companion.EXTRA_WARNINGS
import balti.migrate.utilities.CommonToolKotlin.Companion.FILE_ERRORLOG
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
import balti.migrate.utilities.CommonToolKotlin.Companion.JOBCODE_PERFORM_ZIP_BATCHING
import balti.migrate.utilities.CommonToolKotlin.Companion.JOBCODE_PERFORM_ZIP_VERIFICATION
import balti.migrate.utilities.CommonToolKotlin.Companion.NOTIFICATION_ID_CANCELLING
import balti.migrate.utilities.CommonToolKotlin.Companion.NOTIFICATION_ID_FINISHED
import balti.migrate.utilities.CommonToolKotlin.Companion.NOTIFICATION_ID_ONGOING
import balti.migrate.utilities.CommonToolKotlin.Companion.PREF_COMPRESSION_LEVEL
import balti.migrate.utilities.CommonToolKotlin.Companion.PREF_DEFAULT_COMPRESSION_LEVEL
import balti.migrate.utilities.CommonToolKotlin.Companion.PREF_DELETE_ERROR_BACKUP
import balti.migrate.utilities.CommonToolKotlin.Companion.PREF_SYSTEM_CHECK
import balti.migrate.utilities.CommonToolKotlin.Companion.TIMEOUT_WAITING_TO_CANCEL_TASK
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList


class BackupServiceKotlin: Service(), OnEngineTaskComplete {

    companion object {

        lateinit var serviceContext: Context
        private set

        var cancelAll = false
        private set
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private var destination = ""
    private var backupName = ""
    private var isBackupInitiated = false

    private val commonTools by lazy { CommonToolKotlin(this) }

    private var progressWriter: BufferedWriter? = null
    private var errorWriter: BufferedWriter? = null

    private var lastTitle = ""
    private var lastLog = ""
    private var lastDeterminateProgress = 0

    private val allErrors by lazy { ArrayList<String>(0) }
    private val criticalErrors by lazy { ArrayList<String>(0) }
    private val allWarnings by lazy { ArrayList<String>(0) }

    private var lastErrorCount = 0

    private var startTime = 0L
    private var endTime = 0L

    private var compressionLevel = 0

    private var cTask: ParentBackupClass? = null
    private var cUpdaterJobCode = 0
    private var cZippingJobCode = 0
    private var cZipVerificationJobCode = 0

    private val extrasFiles by lazy { ArrayList<File>(0) }

    private val isSettingsNull : Boolean
        get() = (dpiText == null && keyboardText == null && adbState == null && fontScale == null)

    private var cBackupName = ""
    private var cDestination = ""
    private var cBatchNumber = 0
    private var cZipBatch: ZipAppBatch? = null

    private val timeStamp by lazy { SimpleDateFormat("yyyy.MM.dd_HH.mm.ss").format(Calendar.getInstance().time)}

    private val contactsBackupName = "Contacts_$timeStamp.vcf"
    private val smsBackupName = "Sms_$timeStamp.sms.db"
    private val callsBackupName = "Calls_$timeStamp.calls.db"

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
        val cpuAbi = Build.SUPPORTED_ABIS[0]
        if (cpuAbi == "x86" || cpuAbi == "x86_64")
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

                        cTask?.let {
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

                intent.getIntExtra(EXTRA_PROGRESS_PERCENTAGE, -1).run {
                    if (this != -1) lastDeterminateProgress = this
                }

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

    private fun startBackup(){
        startTime = timeInMillis()

        isBackupInitiated = true

        cBackupName = backupName
        cDestination = destination

        cancelAll = false
        AppInstance.notificationManager.cancelAll()

        doFallThroughJob(JOBCODE_PEFORM_SYSTEM_TEST)
    }

    private fun getBackupIntentData(): BackupIntentData {

        fun resetDest() { cDestination = destination; cBackupName = backupName }

        if (cZipBatch == null) resetDest()
        else {
            cZipBatch?.run {
                if (partName != ""){
                    cDestination = "$destination/$backupName"
                    cBackupName = partName
                }
                else resetDest()
            }
        }

        return BackupIntentData(cBackupName, cDestination).apply {
            cZipBatch?.run {
                if (partName != "")
                    setErrorTag("[${cBatchNumber}/${zipBatches.size}]")
            }
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

        commonTools.LBM?.registerReceiver(progressReceiver, IntentFilter(ACTION_BACKUP_PROGRESS))
        commonTools.LBM?.registerReceiver(cancelReceiver, IntentFilter(ACTION_BACKUP_CANCEL))
        registerReceiver(cancelReceiver, IntentFilter(ACTION_BACKUP_CANCEL))
        commonTools.LBM?.registerReceiver(requestProgressReceiver, IntentFilter(ACTION_REQUEST_BACKUP_DATA))

        startForeground(NOTIFICATION_ID_ONGOING, loadingNotification)

    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        intent?.run {
            try {
                if (!isBackupInitiated) {
                    destination = getStringExtra(EXTRA_DESTINATION)
                    backupName = getStringExtra(EXTRA_BACKUP_NAME)
                    startBackup()
                }
            }
            catch (e: Exception){
                e.printStackTrace()
                addError(e.message.toString())
                backupFinished("${getString(R.string.errorStartingBackup)}: ${e.message}")
            }
        }

        return super.onStartCommand(intent, flags, startId)
    }

    private fun doFallThroughJob(jobCode: Int){

        var fallThrough = false
        val bd = getBackupIntentData()

        fun doJob(jCode: Int, workingObject: Any?){

            if (!cancelAll && (fallThrough || jobCode == jCode)) {

                fallThrough = true
                workingObject?.let {

                    cTask = try {
                        when (jCode) {
                            JOBCODE_PEFORM_SYSTEM_TEST -> if (sharedPrefs.getBoolean(PREF_SYSTEM_CHECK, true)) {
                                File(cDestination, cBackupName).mkdirs()
                                SystemTestingEngine(jCode, bd, busyboxBinaryPath)
                            } else null
                            JOBCODE_PEFORM_BACKUP_CONTACTS -> ContactsBackupEngine(jCode, bd, workingObject as ArrayList<ContactsDataPacketKotlin>, contactsBackupName)
                            JOBCODE_PEFORM_BACKUP_SMS -> SmsBackupEngine(jCode, bd, workingObject as ArrayList<SmsDataPacketKotlin>, smsBackupName)
                            JOBCODE_PEFORM_BACKUP_CALLS -> CallsBackupEngine(jCode, bd, workingObject as ArrayList<CallsDataPacketsKotlin>, callsBackupName)
                            JOBCODE_PEFORM_BACKUP_WIFI -> WifiBackupEngine(jCode, bd, workingObject as WifiDataPacket)
                            JOBCODE_PEFORM_BACKUP_SETTINGS -> SettingsBackupEngine(jCode, bd, dpiText, adbState, fontScale, keyboardText)
                            JOBCODE_PERFORM_APP_BACKUP -> AppBackupEngine(jCode, bd, workingObject as ArrayList<AppPacket>, doBackupInstallers, busyboxBinaryPath)
                            JOBCODE_PERFORM_ZIP_BATCHING -> {
                                (workingObject as Array<*>).let {
                                    val ap = it[0] as ArrayList<AppPacket>
                                    val ef = it[1] as ArrayList<File>
                                    MakeZipBatch(jCode, bd, ap, ef)
                                }
                            }
                            else -> null
                        }
                    } catch (e: Exception){
                        e.printStackTrace()
                        addError("$ERR_BACKUP_SERVICE_ERROR${bd.batchErrorTag}: DO_JOB ${e.message}")
                        null
                    }

                    fallThrough = cTask == null
                    cTask?.executeOnExecutor(THREAD_POOL_EXECUTOR)
                }
            }
        }

        doJob(JOBCODE_PEFORM_SYSTEM_TEST, Any())
        doJob(JOBCODE_PEFORM_BACKUP_CONTACTS, contactsList.let { if (it.isNotEmpty()) it else null })
        doJob(JOBCODE_PEFORM_BACKUP_SMS, smsList.let { if (it.isNotEmpty()) it else null })
        doJob(JOBCODE_PEFORM_BACKUP_CALLS, callsList.let { if (it.isNotEmpty()) it else null })
        doJob(JOBCODE_PEFORM_BACKUP_WIFI, wifiData)
        doJob(JOBCODE_PEFORM_BACKUP_SETTINGS, if (isSettingsNull) null else Any())

        doJob(JOBCODE_PERFORM_APP_BACKUP, appPackets.let { if (it.isNotEmpty()) it else null })

        // if app backup works, app verification will be triggered from callback
        // if no app to backup, fall through to zip batching

        doJob(JOBCODE_PERFORM_ZIP_BATCHING, if (appPackets.isNotEmpty() || extrasFiles.isNotEmpty()) arrayOf(appPackets, extrasFiles) else null)

        // fall through logic ends here. All further engines to be manually called from callback
    }

    override fun onComplete(jobCode: Int, jobErrors: ArrayList<String>, jobWarnings: ArrayList<String>, jobResults: Any?, jobSuccess: Boolean) {

        if (cancelAll) return

        try {

            allWarnings.addAll(jobWarnings)

            if (jobCode in arrayOf(
                            JOBCODE_PEFORM_BACKUP_CONTACTS,
                            JOBCODE_PEFORM_BACKUP_SMS,
                            JOBCODE_PEFORM_BACKUP_CALLS,
                            JOBCODE_PEFORM_BACKUP_WIFI,
                            JOBCODE_PEFORM_BACKUP_SETTINGS
                    )) {
                if (jobResults == null || jobResults !is Array<*>){
                    val label = if (cZipBatch != null) "[${cZipBatch!!.partName}]" else ""
                    addError("${getString(R.string.improper_result_received)}$label: $jobCode : ${jobResults.toString()}")
                }
                else jobResults.let { extrasFiles.addAll(it as Array<File>) }
            }

            if (jobCode in arrayOf(JOBCODE_PERFORM_APP_BACKUP, JOBCODE_PERFORM_APP_BACKUP_VERIFICATION))
                addError(jobErrors, false)
            else addError(jobErrors)

            when (jobCode) {

                JOBCODE_PEFORM_SYSTEM_TEST -> {
                    if (!jobSuccess) {
                        backupFinished(getString(R.string.backup_error_system_check_failed))
                    } else {
                        doFallThroughJob(JOBCODE_PEFORM_BACKUP_CONTACTS)
                    }
                }

                JOBCODE_PEFORM_BACKUP_CONTACTS -> {
                    doFallThroughJob(JOBCODE_PEFORM_BACKUP_SMS)
                }

                JOBCODE_PEFORM_BACKUP_SMS -> {
                    doFallThroughJob(JOBCODE_PEFORM_BACKUP_CALLS)
                }

                JOBCODE_PEFORM_BACKUP_CALLS -> {
                    doFallThroughJob(JOBCODE_PEFORM_BACKUP_WIFI)
                }

                JOBCODE_PEFORM_BACKUP_WIFI -> {
                    doFallThroughJob(JOBCODE_PEFORM_BACKUP_SETTINGS)
                }

                JOBCODE_PEFORM_BACKUP_SETTINGS -> {
                    doFallThroughJob(JOBCODE_PERFORM_APP_BACKUP)
                }

                JOBCODE_PERFORM_APP_BACKUP -> {
                    runConditionalTask(JOBCODE_PERFORM_APP_BACKUP_VERIFICATION)
                }

                JOBCODE_PERFORM_APP_BACKUP_VERIFICATION -> {
                    doFallThroughJob(JOBCODE_PERFORM_ZIP_BATCHING)
                }

                JOBCODE_PERFORM_ZIP_BATCHING -> {
                    commonTools.tryIt {
                    }
                    if (jobSuccess) {
                        zipBatches.clear()
                        zipBatches.addAll(jobResults as ArrayList<ZipAppBatch>)
                        runNextZipBatch(lastErrorCount == criticalErrors.size)
                    }
                    else backupFinished(getString(R.string.failed_to_make_batches))
                }

                cUpdaterJobCode -> {
                    if (jobSuccess)
                        runConditionalTask(JOBCODE_PERFORM_ZIP_BACKUP)
                    else runNextZipBatch()
                }

                cZippingJobCode -> {
                    if (jobSuccess) {
                        val result = jobResults as Array<*>
                        val zippedFiles = result[0] as ArrayList<String>
                        val fileList = result[1] as File?
                        runConditionalTask(JOBCODE_PERFORM_ZIP_VERIFICATION, zippedFiles, fileList)
                    }
                    else runNextZipBatch()
                }

                cZipVerificationJobCode -> {
                    runNextZipBatch(lastErrorCount == criticalErrors.size)
                }
            }
        }
        catch (e: Exception){
            e.printStackTrace()
            addError(e.message.toString())
            backupFinished("")
        }
    }

    private fun runNextZipBatch(isThisBatchSuccessful: Boolean = false){

        if (cancelAll) return

        val label = if (cZipBatch != null) "[${cZipBatch!!.partName}]" else ""
        if (!isThisBatchSuccessful)
            addError("$ERR_BACKUP_SERVICE_ERROR$label: " +
                    "${getString(R.string.errors_in_batch)} ${criticalErrors.size - lastErrorCount}")

        lastErrorCount = criticalErrors.size

        if (cBatchNumber < zipBatches.size) {
            cZipBatch = zipBatches[cBatchNumber]
            ++cBatchNumber
            /*if (cZipBatch == null)
                zipBatches[cBatchNumber]
            else {
                ++cBatchNumber
                zipBatches[cBatchNumber]
            }*/
            runConditionalTask(JOBCODE_PERFORM_UPDATER_SCRIPT)
        }
        else backupFinished("")
    }

    private fun runConditionalTask(jobCode: Int, zipListIfAny: ArrayList<String>? = null, fileListIfAny: File? = null){

        val bd = getBackupIntentData()
        var task : ParentBackupClass? = null

        if (cancelAll) return

        try {

            when (jobCode) {

                JOBCODE_PERFORM_APP_BACKUP_VERIFICATION ->
                    task = VerificationEngine(jobCode, bd, appPackets, busyboxBinaryPath)


                JOBCODE_PERFORM_UPDATER_SCRIPT -> {
                    cZipBatch?.let {
                        cUpdaterJobCode = jobCode + cBatchNumber
                        task = UpdaterScriptMakerEngine(cUpdaterJobCode, bd, it, timeStamp)
                    }
                    if (cZipBatch == null)
                        throw Exception("${getString(R.string.zip_batch_null_updater)}: [$cBatchNumber]")
                }

                JOBCODE_PERFORM_ZIP_BACKUP -> {
                    cZipBatch?.let {
                        cZippingJobCode = jobCode + cBatchNumber
                        task = ZippingEngine(cZippingJobCode, bd, it)
                    }
                    if (cZipBatch == null)
                        throw Exception("${getString(R.string.zip_batch_null_zipping_engine)}: [$cBatchNumber]")
                }

                JOBCODE_PERFORM_ZIP_VERIFICATION -> {
                    cZipBatch?.let {
                        cZipVerificationJobCode = jobCode + cBatchNumber
                        task = ZipVerificationEngine(cZipVerificationJobCode, bd,
                                zipListIfAny!!, File(cDestination, "$cBackupName.zip"), fileListIfAny)
                    }
                    if (cZipBatch == null)
                        throw Exception("${getString(R.string.zip_batch_null_zip_verification)}: [$cBatchNumber]")
                }
            }

            task?.run {
                executeOnExecutor(THREAD_POOL_EXECUTOR)
                cTask = this
            }

        } catch (e: Exception) {

            e.printStackTrace()
            addError("$ERR_BACKUP_SERVICE_ERROR${bd.batchErrorTag}: $ERR_CONDITIONAL_TASK ${e.message}")

            // go to next job
            doFallThroughJob(JOBCODE_PERFORM_ZIP_BATCHING)
        }
    }

    private fun backupFinished(errorTitle: String){

        val title = when {
            errorTitle != "" -> errorTitle
            criticalErrors.isNotEmpty() -> getString(R.string.backupFinishedWithErrors)
            allWarnings.isNotEmpty() -> getString(R.string.backupFinishedWithWarnings)
            else -> getString(R.string.noErrors)
        }

        try {

            if (allWarnings.size == 0) {
                errorWriter?.write("--- No warnings! ---\n")
            } else {
                errorWriter?.write("--- All warnings ---\n\n")
                for (w in allWarnings) {
                    errorWriter?.write("$w\n")
                }
            }

            errorWriter?.write("\n")

            if (allErrors.size == 0 && errorTitle == "") {
                errorWriter?.write("--- No errors! ---\n")
            } else {
                errorWriter?.write("--- All errors ---\n\n")
                if (errorTitle != "") errorWriter?.write("$errorTitle\n\n")
                for (e in allErrors) {
                    errorWriter?.write("$e\n")
                }
            }
            errorWriter?.write("\n--- Backup Name : $backupName ---\n")
            if (cancelAll) errorWriter?.write("--- Cancelled! ---\n")
            errorWriter?.write("--- Migrate version ${getString(R.string.current_version_name)} ---\n")

            progressWriter?.write("\n--- Backup Name : $backupName ---\n")
            progressWriter?.write("--- Total parts : ${zipBatches.size} ---\n")
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
                    putStringArrayListExtra(EXTRA_WARNINGS, allWarnings)
                    putExtra(EXTRA_IS_CANCELLED, cancelAll)
                    putExtra(EXTRA_TOTAL_TIME, endTime - startTime)
                    putExtra(EXTRA_PROGRESS_PERCENTAGE, if (criticalErrors.size == 0 && !cancelAll) 100 else lastDeterminateProgress)
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

        if (cancelAll || ((errorTitle != "" || criticalErrors.size != 0) && sharedPrefs.getBoolean(PREF_DELETE_ERROR_BACKUP, true)))
            commonTools.dirDelete("$destination/$backupName")

        appPackets.clear()
        zipBatches.clear()
        contactsList.clear()
        callsList.clear()
        smsList.clear()
        dpiText = null
        keyboardText = null
        adbState = null
        fontScale = null
        wifiData = null
        doBackupInstallers = false

        stopSelf()
    }

    private fun timeInMillis() = Calendar.getInstance().timeInMillis

    override fun onDestroy() {
        super.onDestroy()
        commonTools.tryIt { commonTools.LBM?.unregisterReceiver(progressReceiver) }
        commonTools.tryIt { commonTools.LBM?.unregisterReceiver(cancelReceiver) }
        commonTools.tryIt { commonTools.LBM?.unregisterReceiver(requestProgressReceiver) }
        commonTools.tryIt { unregisterReceiver(cancelReceiver) }

        commonTools.tryIt { cTask?.cancel(true) }

        commonTools.tryIt { progressWriter?.close() }
        commonTools.tryIt { errorWriter?.close() }
    }
}