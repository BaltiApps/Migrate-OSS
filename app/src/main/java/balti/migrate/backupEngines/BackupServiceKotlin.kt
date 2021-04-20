package balti.migrate.backupEngines

import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import balti.filex.FileX
import balti.filex.FileXInit
import balti.migrate.AppInstance
import balti.migrate.AppInstance.Companion.CACHE_DIR
import balti.migrate.AppInstance.Companion.adbState
import balti.migrate.AppInstance.Companion.appPackets
import balti.migrate.AppInstance.Companion.callsList
import balti.migrate.AppInstance.Companion.contactsList
import balti.migrate.AppInstance.Companion.doBackupInstallers
import balti.migrate.AppInstance.Companion.dpiText
import balti.migrate.AppInstance.Companion.fontScale
import balti.migrate.AppInstance.Companion.keyboardText
import balti.migrate.AppInstance.Companion.smsList
import balti.migrate.AppInstance.Companion.wifiData
import balti.migrate.AppInstance.Companion.zipBatches
import balti.migrate.R
import balti.migrate.backupEngines.containers.AppApkFiles
import balti.migrate.backupEngines.containers.BackupIntentData
import balti.migrate.backupEngines.containers.ZipAppBatch
import balti.migrate.backupEngines.engines.*
import balti.migrate.backupEngines.utils.OnEngineTaskComplete
import balti.migrate.extraBackupsActivity.apps.containers.AppPacket
import balti.migrate.extraBackupsActivity.apps.containers.MDP_Packet
import balti.migrate.extraBackupsActivity.calls.containers.CallsDataPacketsKotlin
import balti.migrate.extraBackupsActivity.contacts.containers.ContactsDataPacketKotlin
import balti.migrate.extraBackupsActivity.sms.containers.SmsDataPacketKotlin
import balti.migrate.extraBackupsActivity.wifi.containers.WifiDataPacket
import balti.migrate.simpleActivities.ProgressShowActivity
import balti.migrate.utilities.CommonToolsKotlin
import balti.migrate.utilities.CommonToolsKotlin.Companion.ACTION_BACKUP_CANCEL
import balti.migrate.utilities.CommonToolsKotlin.Companion.ACTION_BACKUP_PROGRESS
import balti.migrate.utilities.CommonToolsKotlin.Companion.ACTION_REQUEST_BACKUP_DATA
import balti.migrate.utilities.CommonToolsKotlin.Companion.ALL_SUPPRESSED_ERRORS
import balti.migrate.utilities.CommonToolsKotlin.Companion.CHANNEL_BACKUP_CANCELLING
import balti.migrate.utilities.CommonToolsKotlin.Companion.CHANNEL_BACKUP_END
import balti.migrate.utilities.CommonToolsKotlin.Companion.CHANNEL_BACKUP_RUNNING
import balti.migrate.utilities.CommonToolsKotlin.Companion.DEBUG_TAG
import balti.migrate.utilities.CommonToolsKotlin.Companion.ERR_BACKUP_SERVICE_ERROR
import balti.migrate.utilities.CommonToolsKotlin.Companion.ERR_BACKUP_SERVICE_INIT
import balti.migrate.utilities.CommonToolsKotlin.Companion.ERR_CONDITIONAL_TASK
import balti.migrate.utilities.CommonToolsKotlin.Companion.ERR_ON_COMPLETE_TASK
import balti.migrate.utilities.CommonToolsKotlin.Companion.EXTRA_BACKUP_NAME
import balti.migrate.utilities.CommonToolsKotlin.Companion.EXTRA_DESTINATION
import balti.migrate.utilities.CommonToolsKotlin.Companion.EXTRA_ERRORS
import balti.migrate.utilities.CommonToolsKotlin.Companion.EXTRA_FLASHER_ONLY
import balti.migrate.utilities.CommonToolsKotlin.Companion.EXTRA_IS_CANCELLED
import balti.migrate.utilities.CommonToolsKotlin.Companion.EXTRA_PROGRESS_PERCENTAGE
import balti.migrate.utilities.CommonToolsKotlin.Companion.EXTRA_PROGRESS_TYPE
import balti.migrate.utilities.CommonToolsKotlin.Companion.EXTRA_PROGRESS_TYPE_CALLS
import balti.migrate.utilities.CommonToolsKotlin.Companion.EXTRA_PROGRESS_TYPE_CONTACTS
import balti.migrate.utilities.CommonToolsKotlin.Companion.EXTRA_PROGRESS_TYPE_FINISHED
import balti.migrate.utilities.CommonToolsKotlin.Companion.EXTRA_PROGRESS_TYPE_SMS
import balti.migrate.utilities.CommonToolsKotlin.Companion.EXTRA_PROGRESS_TYPE_WAITING_TO_CANCEL
import balti.migrate.utilities.CommonToolsKotlin.Companion.EXTRA_TASKLOG
import balti.migrate.utilities.CommonToolsKotlin.Companion.EXTRA_TITLE
import balti.migrate.utilities.CommonToolsKotlin.Companion.EXTRA_TOTAL_TIME
import balti.migrate.utilities.CommonToolsKotlin.Companion.EXTRA_WARNINGS
import balti.migrate.utilities.CommonToolsKotlin.Companion.EXTRA_ZIP_NAMES
import balti.migrate.utilities.CommonToolsKotlin.Companion.FILE_ERRORLOG
import balti.migrate.utilities.CommonToolsKotlin.Companion.FILE_MDP_PACKAGES
import balti.migrate.utilities.CommonToolsKotlin.Companion.FILE_PROGRESSLOG
import balti.migrate.utilities.CommonToolsKotlin.Companion.FILE_RAW_LIST
import balti.migrate.utilities.CommonToolsKotlin.Companion.JOBCODE_PEFORM_BACKUP_CALLS
import balti.migrate.utilities.CommonToolsKotlin.Companion.JOBCODE_PEFORM_BACKUP_CONTACTS
import balti.migrate.utilities.CommonToolsKotlin.Companion.JOBCODE_PEFORM_BACKUP_SETTINGS
import balti.migrate.utilities.CommonToolsKotlin.Companion.JOBCODE_PEFORM_BACKUP_SMS
import balti.migrate.utilities.CommonToolsKotlin.Companion.JOBCODE_PEFORM_BACKUP_WIFI
import balti.migrate.utilities.CommonToolsKotlin.Companion.JOBCODE_PEFORM_SYSTEM_TEST
import balti.migrate.utilities.CommonToolsKotlin.Companion.JOBCODE_PERFORM_APP_BACKUP
import balti.migrate.utilities.CommonToolsKotlin.Companion.JOBCODE_PERFORM_APP_BACKUP_VERIFICATION
import balti.migrate.utilities.CommonToolsKotlin.Companion.JOBCODE_PERFORM_MDP
import balti.migrate.utilities.CommonToolsKotlin.Companion.JOBCODE_PERFORM_UPDATER_SCRIPT
import balti.migrate.utilities.CommonToolsKotlin.Companion.JOBCODE_PERFORM_ZIP_BACKUP
import balti.migrate.utilities.CommonToolsKotlin.Companion.JOBCODE_PERFORM_ZIP_BATCHING
import balti.migrate.utilities.CommonToolsKotlin.Companion.JOBCODE_PERFORM_ZIP_VERIFICATION
import balti.migrate.utilities.CommonToolsKotlin.Companion.NOTIFICATION_ID_CANCELLING
import balti.migrate.utilities.CommonToolsKotlin.Companion.NOTIFICATION_ID_FINISHED
import balti.migrate.utilities.CommonToolsKotlin.Companion.NOTIFICATION_ID_ONGOING
import balti.migrate.utilities.CommonToolsKotlin.Companion.PREF_COMPRESSION_LEVEL
import balti.migrate.utilities.CommonToolsKotlin.Companion.PREF_DEFAULT_COMPRESSION_LEVEL
import balti.migrate.utilities.CommonToolsKotlin.Companion.PREF_DELETE_ERROR_BACKUP
import balti.migrate.utilities.CommonToolsKotlin.Companion.PREF_IGNORE_APP_CACHE
import balti.migrate.utilities.CommonToolsKotlin.Companion.PREF_SYSTEM_CHECK
import balti.migrate.utilities.CommonToolsKotlin.Companion.TIMEOUT_WAITING_TO_CANCEL_TASK
import balti.module.baltitoolbox.functions.FileHandlers.unpackAssetToInternal
import balti.module.baltitoolbox.functions.Misc.doBackgroundTask
import balti.module.baltitoolbox.functions.Misc.makeNotificationChannel
import balti.module.baltitoolbox.functions.Misc.tryIt
import balti.module.baltitoolbox.functions.SharedPrefs.getPrefBoolean
import balti.module.baltitoolbox.functions.SharedPrefs.getPrefInt
import balti.module.baltitoolbox.jobHandlers.AsyncCoroutineTask
import java.io.BufferedWriter
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList


class BackupServiceKotlin: Service(), OnEngineTaskComplete {

    companion object {

        lateinit var serviceContext: Context
            private set

        var cancelAll = false
            private set

        var flasherOnly = false
            private set
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private var destination = ""
    private var backupName = ""
    private var isBackupInitiated = false

    private val commonTools by lazy { CommonToolsKotlin(this) }

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

    private val extrasFiles by lazy { ArrayList<FileX>(0) }

    private val isSettingsNull : Boolean
        get() = (dpiText == null && keyboardText == null && adbState == null && fontScale == null)

    /**
    * FOR FILEX TRADITIONAL:
    * by default:
    * [cDestination] -> Full canonical path to root of the backup location, i.e [Internal Storage or SD CARD]/Migrate
    * [cBackupName] -> Name of the backup
    * for batches:
    * [cDestination] -> Canonical path to [Internal Storage or SD CARD]/Migrate/<backup_name>
    * [cBackupName] -> Part name
    *
    * FOR NON-TRADITIONAL FILEX
    * by default:
    * [cDestination] -> blank
    * [cBackupName] -> Name of backup
    * for batches:
    * [cDestination] -> Name of backup
    * [cBackupName] -> Part name
    */

    private var cBackupName = ""
    private var cDestination = ""
    private var cBatchNumber = 0
    private var cZipBatch: ZipAppBatch? = null

    private val zipParentPaths by lazy { ArrayList<String>(0) }

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
        (if (cpuAbi == "x86" || cpuAbi == "x86_64")
            unpackAssetToInternal("busybox-x86", "busybox")
        else unpackAssetToInternal("busybox")).apply {
            tryIt { FileX.new(this, true).file.setExecutable(true) }
        }
    }

    private val cancelReceiver by lazy {
        object : BroadcastReceiver(){
            override fun onReceive(context: Context?, intent: Intent?) {
                tryIt {

                    cancelAll = true

                    commonTools.LBM?.sendBroadcast(Intent(ACTION_BACKUP_PROGRESS)
                            .apply {
                                putExtra(EXTRA_PROGRESS_TYPE, EXTRA_PROGRESS_TYPE_WAITING_TO_CANCEL)
                                putExtra(EXTRA_TITLE, getString(R.string.cancelling))
                            }
                    )
                    AppInstance.notificationManager.notify(NOTIFICATION_ID_CANCELLING, cancellingNotification)

                    doBackgroundTask({

                        cTask?.let {
                            while (it.status !in arrayOf(AsyncCoroutineTask.FINISHED, AsyncCoroutineTask.CANCELLED)) {
                                tryIt { Thread.sleep(100) }
                            }
                            tryIt { Thread.sleep(TIMEOUT_WAITING_TO_CANCEL_TASK) }
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

                intent.getStringExtra(EXTRA_TITLE)?.trim()?.run {
                    if (this != lastTitle) {
                        progressWriter?.write("\n$this\n")
                        lastTitle = this
                    }
                }

                if (intent.hasExtra(EXTRA_TASKLOG)){
                    intent.getStringExtra(EXTRA_TASKLOG)?.run {
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

        FileX.new(CACHE_DIR, FILE_RAW_LIST, true).run { if (exists()) delete() }

        doFallThroughJob(JOBCODE_PEFORM_SYSTEM_TEST)
    }

    private fun getBackupIntentData(): BackupIntentData {

        fun resetDest() {
            if (FileXInit.isTraditional) {
                cDestination = destination; cBackupName = backupName
            } else {
                cDestination = ""; cBackupName = backupName
            }
        }

        if (cZipBatch == null) resetDest()
        else {
            cZipBatch?.run {
                if (partName != ""){
                    if (FileXInit.isTraditional) {
                        cDestination = "$destination/$backupName"
                        cBackupName = partName
                    }
                    else {
                        cDestination = backupName
                        cBackupName = partName
                    }
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

        tryIt {
            compressionLevel = getPrefInt(PREF_COMPRESSION_LEVEL, PREF_DEFAULT_COMPRESSION_LEVEL)
        }

        tryIt {
            progressWriter = BufferedWriter(OutputStreamWriter(FileX.new(CACHE_DIR, FILE_PROGRESSLOG, true).outputStream()))
            errorWriter = BufferedWriter(OutputStreamWriter(FileX.new(CACHE_DIR, FILE_ERRORLOG, true).outputStream()))
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            makeNotificationChannel(CHANNEL_BACKUP_RUNNING, CHANNEL_BACKUP_RUNNING, NotificationManager.IMPORTANCE_LOW)
            makeNotificationChannel(CHANNEL_BACKUP_END, CHANNEL_BACKUP_END, NotificationManager.IMPORTANCE_HIGH)
            makeNotificationChannel(CHANNEL_BACKUP_CANCELLING, CHANNEL_BACKUP_CANCELLING, NotificationManager.IMPORTANCE_MIN)
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
                    destination = getStringExtra(EXTRA_DESTINATION).toString()
                    backupName = getStringExtra(EXTRA_BACKUP_NAME).toString()
                    flasherOnly = getBooleanExtra(EXTRA_FLASHER_ONLY, false)
                    startBackup()
                }
            }
            catch (e: Exception){
                e.printStackTrace()
                addError("$ERR_BACKUP_SERVICE_INIT: ${e.message.toString()}")
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
                            JOBCODE_PEFORM_SYSTEM_TEST -> if (getPrefBoolean(PREF_SYSTEM_CHECK, true)) {
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
                                    val appPackets = it[0] as ArrayList<AppPacket>
                                    val extraFiles = it[1] as ArrayList<FileX>
                                    MakeZipBatch(jCode, bd, appPackets, extraFiles)
                                }
                            }
                            else -> null
                        }
                    } catch (e: Exception){
                        e.printStackTrace()
                        addError("$ERR_BACKUP_SERVICE_ERROR${bd.batchErrorTag}: $jCode: DO_JOB ${e.message}")
                        null
                    }

                    fallThrough = cTask == null
                    cTask?.run {
                        FileX.new("$cDestination/$cBackupName").mkdirs()
                        execute()
                    }
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
                    addError("${getString(R.string.improper_result_received)}$label: $jobCode: ${jobResults.toString()}")
                }
                else jobResults.let { extrasFiles.addAll(it as Array<FileX>) }
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
                    if (CommonToolsKotlin.IS_OTHER_APP_DATA_VISIBLE)
                        runConditionalTask(JOBCODE_PERFORM_APP_BACKUP_VERIFICATION)
                    else runConditionalTask(JOBCODE_PERFORM_MDP)
                }

                JOBCODE_PERFORM_MDP ->
                    runConditionalTask(JOBCODE_PERFORM_APP_BACKUP_VERIFICATION)

                JOBCODE_PERFORM_APP_BACKUP_VERIFICATION -> {
                    doFallThroughJob(JOBCODE_PERFORM_ZIP_BATCHING)
                }

                JOBCODE_PERFORM_ZIP_BATCHING -> {
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
                        val fileList = result[1] as FileX?
                        val dest = result[2] as String

                        zipParentPaths.add(dest)

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
            addError("$ERR_BACKUP_SERVICE_ERROR: $jobCode: $ERR_ON_COMPLETE_TASK ${e.message}")
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
            val appApkList: List<AppApkFiles> = cZipBatch?.zipAppPackets?.map { it.appApkFiles }?: listOf()
            ++cBatchNumber
            runConditionalTask(JOBCODE_PERFORM_UPDATER_SCRIPT, appApkList = appApkList)
        }
        else backupFinished("")
    }

    private fun runConditionalTask(jobCode: Int, zipListIfAny: ArrayList<String>? = null, fileListIfAny: FileX? = null, appApkList: List<AppApkFiles> = listOf()){

        val bd = getBackupIntentData()
        var task : ParentBackupClass? = null

        if (cancelAll) return

        try {

            when (jobCode) {

                JOBCODE_PERFORM_MDP -> {
                    val mdpPacket = MDP_Packet(FILE_MDP_PACKAGES, appPackets.filter { it.DATA }.map { it.packageName })
                    task = MDPEngine(jobCode, bd, busyboxBinaryPath, getPrefBoolean(PREF_IGNORE_APP_CACHE, false), mdpPacket)
                }

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
                                zipListIfAny!!, FileX.new(cDestination, "$cBackupName.zip"), fileListIfAny)
                    }
                    if (cZipBatch == null)
                        throw Exception("${getString(R.string.zip_batch_null_zip_verification)}: [$cBatchNumber]")
                }
            }

            task?.run {
                execute()
                cTask = this
            }

        } catch (e: Exception) {

            e.printStackTrace()
            addError("$ERR_BACKUP_SERVICE_ERROR${bd.batchErrorTag}: $jobCode: $ERR_CONDITIONAL_TASK ${e.message}")

            // go to next job
            runNextZipBatch()
        }
    }

    private fun backupFinished(errorTitle: String) {

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
        } catch (e: Exception) {
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
                    putExtra(EXTRA_BACKUP_NAME, backupName)
                    tryIt {
                        putStringArrayListExtra(EXTRA_ZIP_NAMES, zipBatches.let { b ->
                            if (b.size == 1) arrayListOf(backupName)
                            else ArrayList(b.map { it.partName })
                        })
                    }
                }

        commonTools.LBM?.sendBroadcast(returnIntent)

        AppInstance.notificationManager.cancel(NOTIFICATION_ID_CANCELLING)

        AppInstance.notificationManager.notify(NOTIFICATION_ID_FINISHED,
                NotificationCompat.Builder(this, CHANNEL_BACKUP_END)
                        .setContentTitle(title)
                        .setSmallIcon(R.drawable.ic_notification_icon)
                        .setContentIntent(
                                PendingIntent.getActivity(serviceContext, CommonToolsKotlin.PENDING_INTENT_REQUEST_ID,
                                        Intent(this, ProgressShowActivity::class.java).putExtras(returnIntent),
                                        PendingIntent.FLAG_UPDATE_CURRENT))
                        .build())

        val errorCondition = errorTitle != "" || criticalErrors.size != 0

        if ((cancelAll || errorCondition) && getPrefBoolean(PREF_DELETE_ERROR_BACKUP, true)) {
            FileX.new(
                    if(FileXInit.isTraditional) "$destination/$backupName" else backupName
            ).run {
                if (CommonToolsKotlin.isDeletable(this)) {
                    Log.d(DEBUG_TAG, "Cleaning up on error or cancel: ${this.absolutePath}")
                    deleteRecursively()
                }
            }
        }
        else if (!cancelAll && !errorCondition) {

            // clean empty folders and other files if remaining if no errors
            zipParentPaths.forEach {

                FileX.new(it).run {
                    if (CommonToolsKotlin.isDeletable(this)) {
                        Log.d(DEBUG_TAG, "Cleaning up : ${this.absolutePath}")
                        deleteRecursively()
                    }
                }
            }
        }

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
        tryIt { commonTools.LBM?.unregisterReceiver(progressReceiver) }
        tryIt { commonTools.LBM?.unregisterReceiver(cancelReceiver) }
        tryIt { commonTools.LBM?.unregisterReceiver(requestProgressReceiver) }
        tryIt { unregisterReceiver(cancelReceiver) }

        tryIt { cTask?.cancel(true) }

        tryIt { progressWriter?.close() }
        tryIt { errorWriter?.close() }
    }
}
