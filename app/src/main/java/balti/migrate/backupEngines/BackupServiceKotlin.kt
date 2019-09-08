package balti.migrate.backupEngines

import android.app.NotificationManager
import android.app.Service
import android.content.*
import android.os.Build
import android.os.IBinder
import android.support.v4.app.NotificationCompat
import balti.migrate.R
import balti.migrate.backupEngines.utils.OnBackupComplete
import balti.migrate.extraBackupsActivity.apps.AppBatch
import balti.migrate.extraBackupsActivity.calls.CallsDataPacketsKotlin
import balti.migrate.extraBackupsActivity.contacts.ContactsDataPacketKotlin
import balti.migrate.extraBackupsActivity.sms.SmsDataPacketKotlin
import balti.migrate.utilities.CommonToolKotlin
import balti.migrate.utilities.CommonToolKotlin.Companion.ACTION_BACKUP_CANCEL
import balti.migrate.utilities.CommonToolKotlin.Companion.ACTION_BACKUP_PROGRESS
import balti.migrate.utilities.CommonToolKotlin.Companion.ACTION_BACKUP_SERVICE_STARTED
import balti.migrate.utilities.CommonToolKotlin.Companion.ACTION_REQUEST_BACKUP_DATA
import balti.migrate.utilities.CommonToolKotlin.Companion.ACTION_START_BATCH_BACKUP
import balti.migrate.utilities.CommonToolKotlin.Companion.BACKUP_NOTIFICATION_ID
import balti.migrate.utilities.CommonToolKotlin.Companion.CHANNEL_BACKUP_END
import balti.migrate.utilities.CommonToolKotlin.Companion.CHANNEL_BACKUP_RUNNING
import balti.migrate.utilities.CommonToolKotlin.Companion.CHANNEL_BACKUP_START
import balti.migrate.utilities.CommonToolKotlin.Companion.EXTRA_APP_LOG
import balti.migrate.utilities.CommonToolKotlin.Companion.EXTRA_APP_NAME
import balti.migrate.utilities.CommonToolKotlin.Companion.EXTRA_ERRORS
import balti.migrate.utilities.CommonToolKotlin.Companion.EXTRA_PROGRESS_TYPE
import balti.migrate.utilities.CommonToolKotlin.Companion.EXTRA_PROGRESS_TYPE_APP_PROGRESS
import balti.migrate.utilities.CommonToolKotlin.Companion.EXTRA_PROGRESS_TYPE_CORRECTING
import balti.migrate.utilities.CommonToolKotlin.Companion.EXTRA_PROGRESS_TYPE_MAKING_APP_SCRIPTS
import balti.migrate.utilities.CommonToolKotlin.Companion.EXTRA_PROGRESS_TYPE_TESTING
import balti.migrate.utilities.CommonToolKotlin.Companion.EXTRA_PROGRESS_TYPE_VERIFYING
import balti.migrate.utilities.CommonToolKotlin.Companion.EXTRA_PROGRESS_TYPE_ZIP_PROGRESS
import balti.migrate.utilities.CommonToolKotlin.Companion.EXTRA_RETRY_LOG
import balti.migrate.utilities.CommonToolKotlin.Companion.EXTRA_SCRIPT_APP_NAME
import balti.migrate.utilities.CommonToolKotlin.Companion.EXTRA_TAR_CHECK_LOG
import balti.migrate.utilities.CommonToolKotlin.Companion.EXTRA_TEST_LOG
import balti.migrate.utilities.CommonToolKotlin.Companion.EXTRA_ZIP_LOG
import balti.migrate.utilities.CommonToolKotlin.Companion.MIGRATE_STATUS
import balti.migrate.utilities.CommonToolKotlin.Companion.PREF_COMPRESSION_LEVEL
import balti.migrate.utilities.CommonToolKotlin.Companion.PREF_DEFAULT_COMPRESSION_LEVEL
import balti.migrate.utilities.CommonToolKotlin.Companion.PREF_FILE_MAIN
import dagger.Module
import dagger.Provides
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter

@Module
class BackupServiceKotlin: Service(), OnBackupComplete {

    private val sharedPrefs by lazy { getSharedPreferences(PREF_FILE_MAIN, Context.MODE_PRIVATE) }

    @Provides
    fun getEngineContext() : Context = this

    @Provides
    fun getPrefs(): SharedPreferences = sharedPrefs

    override fun onBind(intent: Intent?): IBinder? = null

    private var cancelAll = false
    private val commonTools by lazy { CommonToolKotlin(this) }

    private var backupEngine: MainBackupEngine? = null

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

    private val appBatchesCopied by lazy { ArrayList<AppBatch>(0) }
    private var backupNameCopied = ""

    private var PREVIOUS_TIME = 0L
    private var runningBatchCount = 0

    private var compressionLevel = 0

    private val cancelReceiver by lazy {
        object : BroadcastReceiver(){
            override fun onReceive(context: Context?, intent: Intent?) {
                commonTools.tryIt {
                    cancelAll = true
                    backupEngine?.cancelTask()
                }
            }
        }
    }

    private val progressReceiver by lazy {
        object : BroadcastReceiver(){
            override fun onReceive(context: Context?, intent: Intent?) {

                if (intent == null || !intent.hasExtra(EXTRA_PROGRESS_TYPE)) return

                toReturnIntent.putExtras(intent)

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
                            }
                        }
                    }
                }

                when (intent.getStringExtra(EXTRA_PROGRESS_TYPE)) {

                    /*EXTRA_PROGRESS_TYPE_FINISHED -> {

                        commonTools.tryIt {

                            if (intent.hasExtra(EXTRA_ERRORS))
                                allErrors.addAll(intent.getStringArrayListExtra(EXTRA_ERRORS))

                            if (intent.getBooleanExtra(EXTRA_IS_FINAL_PROCESS, false)) {

                                if (intent.hasExtra(EXTRA_FINISHED_MESSAGE))
                                    progressWriter?.write("\n\n" + intent.getStringExtra(EXTRA_FINISHED_MESSAGE) + "\n\n\n")

                                allErrors.forEach {
                                    commonTools.tryIt { errorWriter?.write(it + "\n") }
                                }

                                commonTools.tryIt {
                                    progressWriter?.write("--->> $backupNameCopied <<---\n")
                                    progressWriter?.write("--- Migrate version " + getString(R.string.current_version_name) + " ---\n")

                                    errorWriter?.write("\n\n--->> $backupNameCopied <<---\n")
                                    errorWriter?.write("--- Migrate version " + getString(R.string.current_version_name) + " ---\n")
                                }

                                commonTools.tryIt {
                                    progressWriter?.close()
                                    errorWriter?.close()
                                }

                                PREVIOUS_TIME = 0
                            } else {
                                PREVIOUS_TIME += intent.getLongExtra(EXTRA_TOTAL_TIME, 0L)
                            }

                            if ((runningBatchCount + 1) < appBatchesCopied.size) {
                                runningBatchCount += 1
                                runNextBatch()
                            }

                        }
                    }*/

                    EXTRA_PROGRESS_TYPE_TESTING -> {
                        if (!startedTest) {
                            progressWriter?.write("\n\n${MIGRATE_STATUS} System test logs\n")
                            startedTest = true
                        }
                        writeLogs(EXTRA_TEST_LOG)
                        if (intent.hasExtra(EXTRA_ERRORS)) {
                            allErrors.addAll(intent.getStringArrayListExtra(EXTRA_ERRORS))
                        }
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
                runNextBatch()
            }
        }
    }

    companion object {
        var destination = ""

        var contactsList = ArrayList<ContactsDataPacketKotlin>(0)
        var doBackupContacts = false

        var callsList = ArrayList<CallsDataPacketsKotlin>(0)
        var doBackupCalls = false

        var smsList = ArrayList<SmsDataPacketKotlin>(0)
        var doBackupSms = false

        var dpiText = ""
        var doBackupDpi = false

        var keyboardText = ""
        var doBackupKeyboard = false

        var adbState = 0
        var doBackupAdb = false

        var fontScale = 0.0
        var doBackupFontScale = false

        var wifiContent = ArrayList<String>(0)
        var doBackupWifi = false

        var doBackupInstallers = false

        fun setBackupBatches(destination: String,
                             contactsList: ArrayList<ContactsDataPacketKotlin>, doBackupContacts: Boolean,
                             callsList: ArrayList<CallsDataPacketsKotlin>, doBackupCalls: Boolean,
                             smsList: ArrayList<SmsDataPacketKotlin>, doBackupSms: Boolean,
                             dpiText: String, doBackupDpi: Boolean,
                             keyboardText: String, doBackupKeyboard: Boolean,
                             adbState: Int, doBackupAdb: Boolean,
                             fontScale: Double, doBackupFontScale: Boolean,
                             wifiContent: ArrayList<String>, doBackupWifi: Boolean,
                             doBackupInstallers: Boolean){

            this.destination = destination

            this.contactsList = contactsList
            this.doBackupContacts = doBackupContacts

            this.callsList = callsList
            this.doBackupCalls = doBackupCalls

            this.smsList = smsList
            this.doBackupSms = doBackupSms

            this.dpiText = dpiText
            this.doBackupDpi = doBackupDpi

            this.keyboardText = keyboardText
            this.doBackupKeyboard = doBackupKeyboard

            this.adbState = adbState
            this.doBackupAdb = doBackupAdb

            this.fontScale = fontScale
            this.doBackupFontScale = doBackupFontScale

            this.wifiContent = wifiContent
            this.doBackupWifi = doBackupWifi

            this.doBackupInstallers = doBackupInstallers
        }

    }

    override fun onCreate() {
        super.onCreate()

        commonTools.tryIt {
            compressionLevel = sharedPrefs.getInt(PREF_COMPRESSION_LEVEL, PREF_DEFAULT_COMPRESSION_LEVEL)
        }

        commonTools.tryIt {
            backupNameCopied = balti.migrate.extraBackupsActivity.ExtraBackupsKotlin.backupName
            appBatchesCopied.addAll(balti.migrate.extraBackupsActivity.ExtraBackupsKotlin.appBatches)
        }

        commonTools.tryIt {
            progressWriter = BufferedWriter(FileWriter(File(externalCacheDir, CommonToolKotlin.FILE_PROGRESSLOG)))
            errorWriter = BufferedWriter(FileWriter(File(externalCacheDir, CommonToolKotlin.FILE_ERRORLOG)))
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            commonTools.makeNotificationChannel(CHANNEL_BACKUP_START, CHANNEL_BACKUP_START, NotificationManager.IMPORTANCE_DEFAULT)
            commonTools.makeNotificationChannel(CHANNEL_BACKUP_RUNNING, CHANNEL_BACKUP_RUNNING, NotificationManager.IMPORTANCE_DEFAULT)
            commonTools.makeNotificationChannel(CHANNEL_BACKUP_END, CHANNEL_BACKUP_END, NotificationManager.IMPORTANCE_DEFAULT)
        }

        val notification = NotificationCompat.Builder(this, CHANNEL_BACKUP_START)
                .setContentTitle(getString(R.string.loading))
                .setSmallIcon(R.drawable.ic_notification_icon)
                .build()

        commonTools.LBM?.registerReceiver(startBatchBackupReceiver, IntentFilter(ACTION_START_BATCH_BACKUP))
        commonTools.LBM?.registerReceiver(progressReceiver, IntentFilter(ACTION_BACKUP_PROGRESS))
        commonTools.LBM?.registerReceiver(cancelReceiver, IntentFilter(ACTION_BACKUP_CANCEL))
        registerReceiver(cancelReceiver, IntentFilter(ACTION_BACKUP_CANCEL))
        commonTools.LBM?.registerReceiver(requestProgressReceiver, IntentFilter(ACTION_REQUEST_BACKUP_DATA))

        startForeground(BACKUP_NOTIFICATION_ID, notification)

        commonTools.LBM?.sendBroadcast(Intent(ACTION_BACKUP_SERVICE_STARTED))

    }

    private fun runNextBatch(){

        if (cancelAll) return

        //TODO("Add batch here")

        commonTools.tryIt {
            progressWriter?.write("\n\n--- Next batch backup: ${runningBatchCount + 1} ---\n\n")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        commonTools.tryIt { commonTools.LBM?.unregisterReceiver(startBatchBackupReceiver) }
        commonTools.tryIt { commonTools.LBM?.unregisterReceiver(progressReceiver) }
        commonTools.tryIt { commonTools.LBM?.unregisterReceiver(cancelReceiver) }
        commonTools.tryIt { commonTools.LBM?.unregisterReceiver(requestProgressReceiver) }
        commonTools.tryIt { unregisterReceiver(cancelReceiver) }

        commonTools.tryIt { progressWriter?.close() }
        commonTools.tryIt { errorWriter?.close() }
    }

    override fun onBackupComplete(jobCode: Int, jobSuccess: Boolean, jobResult: Any?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}