package balti.migrate.utilities

import android.app.Activity
import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.AsyncTask
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import balti.migrate.AppInstance
import balti.migrate.R
import balti.migrate.simpleActivities.PrivacyPolicy
import balti.module.baltitoolbox.functions.Misc.isPackageInstalled
import balti.module.baltitoolbox.functions.Misc.openWebLink
import balti.module.baltitoolbox.functions.Misc.playStoreLink
import balti.module.baltitoolbox.functions.Misc.tryIt
import balti.module.baltitoolbox.functions.SharedPrefs.getPrefString
import kotlinx.android.synthetic.main.error_report_layout.view.*
import java.io.*

class CommonToolsKotlin(val context: Context? = null) {

    companion object {

        val THIS_VERSION = 31
        val LAST_SUPPORTED_ANDROID_API = 29

        val DEBUG_TAG = "migrate_tag"

        val NOTIFICATION_ID_ONGOING = 129
        val NOTIFICATION_ID_FINISHED = 130
        val NOTIFICATION_ID_CANCELLING = 131

        val PENDING_INTENT_REQUEST_ID = 913
        val PENDING_INTENT_BACKUP_CANCEL_ID = 912

        val DEFAULT_INTERNAL_STORAGE_DIR = "/sdcard/Migrate"
        val MIGRATE_CACHE_DEFAULT = "/data/local/tmp/migrate_cache"

        val DIR_MANUAL_CONFIGS = "manualConfigs"
        val FILE_MIGRATE_CACHE_MANUAL = "MIGRATE_CACHE_MANUAL"
        val FILE_SYSTEM_MANUAL = "SYSTEM_MANUAL"
        val FILE_BUILDPROP_MANUAL = "BUILDPROP_MANUAL"

        val FILE_PROGRESSLOG = "progressLog.txt"
        val FILE_ERRORLOG = "errorLog.txt"
        val FILE_DEVICE_INFO = "device_info.txt"
        val FILE_PREFIX_BACKUP_SCRIPT = "the_backup_script"
        val FILE_PREFIX_RETRY_SCRIPT = "retry_script"
        val FILE_PREFIX_TAR_CHECK = "tar_check"

        val FILE_ZIP_NAME_EXTRAS = "Extras"

        val FILE_FILE_LIST = "fileList.txt"
        val FILE_RAW_LIST = "rawList.txt"
        val FILE_PACKAGE_DATA = "package-data.txt"

        val CHANNEL_BACKUP_END = "Backup finished notification"
        val CHANNEL_BACKUP_RUNNING = "Backup running notification"
        val CHANNEL_BACKUP_CANCELLING = "Cancelling current backup"

        val ACTION_BACKUP_PROGRESS = "Migrate progress broadcast"
        val ACTION_BACKUP_CANCEL = "Migrate backup cancel broadcast"
        val ACTION_REQUEST_BACKUP_DATA = "get data"

        val EXTRA_SHOW_FIRST_WARNING = "show_first_warning"

        val EXTRA_PROGRESS_TYPE = "type"
        val EXTRA_TITLE = "title"
        val EXTRA_SUBTASK = "subtask"
        val EXTRA_TASKLOG = "tasklog"
        val EXTRA_PROGRESS_PERCENTAGE = "progress"
        val EXTRA_ZIP_NAMES = "zip_names"

        val EXTRA_PROGRESS_TYPE_TESTING = "TESTING_SYSTEM"
        val EXTRA_PROGRESS_TYPE_CONTACTS = "contacts_progress"
        val EXTRA_PROGRESS_TYPE_SMS = "sms_progress"
        val EXTRA_PROGRESS_TYPE_CALLS = "calls_progress"
        val EXTRA_PROGRESS_TYPE_WIFI = "wifi_progress"
        val EXTRA_PROGRESS_TYPE_SETTINGS = "settings_progress"
        val EXTRA_PROGRESS_TYPE_MAKING_APP_SCRIPTS = "making_app_backup_scripts"
        val EXTRA_PROGRESS_TYPE_APP_PROGRESS = "app_progress"
        val EXTRA_PROGRESS_TYPE_VERIFYING = "verifying_backups"
        val EXTRA_PROGRESS_TYPE_CORRECTING = "correcting_errors"
        val EXTRA_PROGRESS_TYPE_UPDATER_SCRIPT = "updater_script_progress"
        val EXTRA_PROGRESS_TYPE_ZIP_PROGRESS = "zip_progress"
        val EXTRA_PROGRESS_TYPE_ZIP_VERIFICATION = "zip_verification_progress"
        val EXTRA_PROGRESS_TYPE_FINISHED = "finished"
        val EXTRA_PROGRESS_TYPE_WAITING_TO_CANCEL = "waiting_to_cancel"
        val EXTRA_PROGRESS_TYPE_MAKING_ZIP_BATCH = "making_zip_batch"

        val EXTRA_BACKUP_NAME = "backupName"
        val EXTRA_DESTINATION = "destination"
        val EXTRA_ACTUAL_DESTINATION = "actualDestination"
        val EXTRA_ERRORS = "errors"
        val EXTRA_WARNINGS = "warnings"
        val EXTRA_TOTAL_TIME = "total_time"
        val EXTRA_IS_CANCELLED = "isCancelled"

        val ERR_ZIP_TRY_CATCH = "ZIP_TRY_CATCH"
        val ERR_VERIFICATION_TRY_CATCH = "VERIFICATION_TRY_CATCH"
        val ERR_CORRECTION_SHELL = "CORRECTION_SHELL"
        val ERR_CORRECTION_SUPPRESSED = "CORRECTION_SUPPRESSED"
        val ERR_CORRECTION_TRY_CATCH = "CORRECTION_TRY_CATCH"
        val ERR_TAR_SHELL = "TAR_ERR"
        val ERR_TAR_SUPPRESSED = "TAR_SUPPRESSED"
        val ERR_TAR_CHECK_TRY_CATCH = "TAR_TRY_CATCH"
        val ERR_SCRIPT_MAKING_TRY_CATCH = "SCRIPT_MAKING_TRY_CATCH"
        val ERR_APP_BACKUP_SHELL = "RUN"
        val ERR_APP_BACKUP_SUPPRESSED = "RUN_SUPPRESSED"
        val ERR_APP_BACKUP_TRY_CATCH = "RUN_TRY_CATCH"
        val ERR_ZIP_VERIFICATION_TRY_CATCH = "ZIP_VERI_TRY_CATCH"
        val ERR_ZIP_ITEM_UNAVAILABLE = "ZIP_ITEM_UNAVAILABLE"
        val ERR_ZIP_FL_ITEM_UNAVAILABLE = "ZIP_FL_ITEM_UNAVAILABLE"
        val ERR_ZIP_FL_UNAVAILABLE = "ZIP_FILELIST_UNAVAILABLE"
        val ERR_ZIP_TOO_BIG = "ZIP_TOO_BIG"
        val ERR_CONTACTS_TRY_CATCH = "CONTACTS_TRY_CATCH"
        val ERR_CALLS_WRITE = "CALLS_WRITE"
        val ERR_CALLS_TRY_CATCH = "CALLS_TRY_CATCH"
        val ERR_CALLS_VERIFY = "CALLS_VERIFY"
        val ERR_CALLS_VERIFY_TRY_CATCH = "CALLS_VERIFY_TRY_CATCH"
        val ERR_SMS_WRITE = "SMS_WRITE"
        val ERR_SMS_TRY_CATCH = "SMS_TRY_CATCH"
        val ERR_SMS_VERIFY = "SMS_VERIFY"
        val ERR_SMS_VERIFY_TRY_CATCH = "SMS_VERIFY_TRY_CATCH"
        val ERR_WIFI_TRY_CATCH = "WIFI_TRY_CATCH"
        val ERR_SETTINGS_TRY_CATCH = "SETTINGS_TRY_CATCH"
        val ERR_TESTING_ERROR = "SYSTEM_TESTING_ERROR"
        val ERR_TESTING_TRY_CATCH = "SYSTEM_TESTING_TRY_CATCH"
        val ERR_UPDATER_TRY_CATCH = "UPDATER_TRY_CATCH"
        val ERR_UPDATER_EXTRACT = "UPDATER_EXTRACT"
        val ERR_UPDATER_CONFIG_FILE = "UPDATER_CONFIG_FILE"
        val ERR_WRITING_RAW_LIST = "ERR_WRITING_RAW_LIST"
        val ERR_ZIP_PACKET_MAKING = "ZIP_PACKET_MAKING"
        val ERR_ZIP_BATCHING = "ZIP_BATCHING"
        val ERR_ZIP_ADDING_EXTRAS = "ZIP_ADDING_EXTRAS"
        val ERR_ZIP_ENGINE_INIT = "ZIP_ENGINE_INIT"
        val ERR_MOVING = "MOVING_FILES"

        val ERR_BACKUP_SERVICE_ERROR = "BACKUP_SERVICE"
        val ERR_CONDITIONAL_TASK = "RUN_CONDITIONAL_TASK"
        val ERR_ON_COMPLETE_TASK = "ON_COMPLETE_TASK"
        val ERR_BACKUP_SERVICE_INIT = "BACKUP_SERVICE_INIT"

        val ALL_SUPPRESSED_ERRORS = arrayOf(ERR_APP_BACKUP_SUPPRESSED, ERR_CORRECTION_SUPPRESSED, ERR_TAR_SUPPRESSED)

        val WARNING_ZIP_BATCH = "ZIP_BATCH_WARNING"
        val WARNING_CALLS = "CALL_VERIFY_WARNING"
        val WARNING_SMS = "SMS_VERIFY_WARNING"
        val WARNING_ZIP_FILELIST_VERIFICATION = "ZIP_FILELIST"
        val WARNING_ZIP_FILELIST_UNAVAILABLE = "ZIP_FILELIST_UNAVAILABLE"
        val WARNING_ZIP_FILELIST_ITEM_UNAVAILABLE = "FILELIST_ITEM_UNAVAILABLE"
        val WARNING_FILE_LIST_COPY = "FILE_LIST_COPY"

        val ALL_WARNINGS = arrayOf(WARNING_ZIP_BATCH, WARNING_CALLS, WARNING_SMS, WARNING_ZIP_FILELIST_VERIFICATION,
                WARNING_ZIP_FILELIST_UNAVAILABLE, WARNING_ZIP_FILELIST_ITEM_UNAVAILABLE, WARNING_FILE_LIST_COPY)

        val PACKAGE_NAME_PLAY_STORE = "com.android.vending"
        val PACKAGE_NAME_FDROID = "org.fdroid.fdroid.privileged"
        val PACKAGE_NAMES_KNOWN = arrayOf(PACKAGE_NAME_PLAY_STORE, PACKAGE_NAME_FDROID)
        val PACKAGE_NAMES_PACKAGE_INSTALLER = arrayOf("com.google.android.packageinstaller")

        val PREF_FILE_APPS = "apps"
        val PREF_FIRST_RUN = "firstRun"
        val PREF_VERSION_CURRENT = "version"
        val PREF_ANDROID_VERSION_WARNING = "android_version_warning"
        val PREF_DEFAULT_BACKUP_PATH = "defaultBackupPath"
        val PREF_ASK_FOR_RATING = "askForRating"
        val PREF_SYSTEM_APPS_WARNING = "system_apps_warning"
        val PREF_CALCULATING_SIZE_METHOD = "calculating_size_method"
        val PREF_TERMINAL_METHOD = 1
        val PREF_ALTERNATE_METHOD = 2
        val PREF_MAX_BACKUP_SIZE = "max_backup_size"
        val PREF_AUTOSELECT_EXTRAS = "autoSelectExtraBackups"
        val PREF_SHOW_STOCK_WARNING = "showStockWarning"
        val PREF_COMPRESSION_LEVEL = "compressionLevel"
        val PREF_DEFAULT_COMPRESSION_LEVEL = 0

        val PREF_NEW_ICON_METHOD = "new_icon_method"
        val PREF_TAR_GZ_INTEGRITY = "tar_integrity"
        val PREF_SMS_VERIFY = "sms_verify"
        val PREF_CALLS_VERIFY = "calls_verify"
        val PREF_SYSTEM_CHECK = "do_system_check"
        val PREF_SEPARATE_EXTRAS_BACKUP = "separate_extras"
        val PREF_FORCE_SEPARATE_EXTRAS_BACKUP = "force_separate_extras"
        val PREF_DELETE_ERROR_BACKUP = "delete_backup_on_error"
        val PREF_USE_SU_FOR_KEYBOARD = "use_su_for_keyboard"
        val PREF_ZIP_VERIFICATION = "do_zip_verification"
        val PREF_FILELIST_IN_ZIP_VERIFICATION = "fileList_in__zip_verification"
        val PREF_SHOW_BACKUP_SUMMARY = "showBackupSummary"

        val PREF_IGNORE_APP_CACHE = "ignore_app_cache"

        val PREF_MANUAL_MIGRATE_CACHE = "manual_migrate_cache"
        val PREF_MANUAL_SYSTEM = "manual_system"
        val PREF_MANUAL_BUILDPROP = "manual_buildProp"

        val PROPERTY_APP_SELECTION = "app"        // used to set property in AppListAdapter
        val PROPERTY_DATA_SELECTION = "data"        // used to set property in AppListAdapter
        val PROPERTY_PERMISSION_SELECTION = "permission"        // used to set property in AppListAdapter

        //extra backups

        val JOBCODE_READ_CONTACTS = 3443
        val JOBCODE_READ_SMS = 2398
        val JOBCODE_READ_SMS_THEN_CALLS = 2399
        val JOBCODE_READ_CALLS = 1109
        val JOBCODE_READ_DPI = 3570
        val JOBCODE_READ_ADB = 2339
        val JOBCODE_READ_WIFI = 1264
        val JOBCODE_READ_FONTSCALE = 9221

        val JOBCODE_LOAD_CONTACTS = 7570
        val JOBCODE_LOAD_SMS = 1944
        val JOBCODE_LOAD_CALLS = 2242
        val JOBCODE_LOAD_KEYBOARDS = 6765
        val JOBCODE_LOAD_INSTALLERS = 8709

        val JOBCODE_MAKE_APP_PACKETS = 6536

        val JOBCODE_PEFORM_SYSTEM_TEST = 10000
        val JOBCODE_PEFORM_BACKUP_CONTACTS = 20000
        val JOBCODE_PEFORM_BACKUP_SMS = 30000
        val JOBCODE_PEFORM_BACKUP_CALLS = 40000
        val JOBCODE_PEFORM_BACKUP_WIFI = 50000
        val JOBCODE_PEFORM_BACKUP_SETTINGS = 60000
        val JOBCODE_PERFORM_APP_BACKUP = 70000
        val JOBCODE_PERFORM_APP_BACKUP_VERIFICATION = 80000
        val JOBCODE_PERFORM_ZIP_BATCHING = 85000
        val JOBCODE_PERFORM_UPDATER_SCRIPT = 90000
        val JOBCODE_PERFORM_ZIP_BACKUP = 100000
        val JOBCODE_PERFORM_ZIP_VERIFICATION = 110000

        val TIMEOUT_WAITING_TO_CANCEL_TASK = 500L
        val TIMEOUT_WAITING_TO_KILL = 3000L

        val CONTACT_PERMISSION = 933
        val SMS_PERMISSION = 944
        val CALLS_PERMISSION = 676
        val SMS_AND_CALLS_PERMISSION = 567

        val BACKUP_NAME_SETTINGS = "settings.json"

        val WIFI_FILE_NAME = "WifiConfigStore.xml"
        val WIFI_FILE_PATH = "/data/misc/wifi/$WIFI_FILE_NAME"
        val WIFI_FILE_NOT_FOUND = "***not_found***"

        val SIMPLE_LOG_VIEWER_HEAD = "slg_head"
        val SIMPLE_LOG_VIEWER_FILEPATH = "slg_filePath"

        // installer list adapter

        val NOT_SET_POSITION = 0
        val PLAY_STORE_POSITION = 1
        val FDROID_POSITION = 2

        val KB_DIVISION_SIZE = 1024

        // main backup engine
        val MIGRATE_STATUS_LABEL = "migrate_status"

        val MIGRATE_STATUS = "MIGRATE_STATUS"

        val REPORTING_EMAIL = "help.baltiapps@gmail.com"
        val TG_LINK = "https://t.me/migrateApp"
        val TG_DEV_LINK = "https://t.me/SayantanRC"

        val TG_CLIENTS = arrayOf(
                "org.telegram.messenger",          // Official Telegram app
                "org.thunderdog.challegram",       // Telegram X
                "org.telegram.plus"                // Plus messenger
        )

        fun isDeletable(f: File): Boolean{
            val d = getPrefString(PREF_DEFAULT_BACKUP_PATH, DEFAULT_INTERNAL_STORAGE_DIR)
            val parentPath = File(d).absolutePath
            return f.absolutePath.startsWith(parentPath)
        }
    }

    var LBM : androidx.localbroadcastmanager.content.LocalBroadcastManager? = null
    val workingContext by lazy { context?: AppInstance.appContext }

    init {
        if (context != null && workingContext is Activity || workingContext is Service)
            LBM = androidx.localbroadcastmanager.content.LocalBroadcastManager.getInstance(workingContext)
    }

    fun reportLogs(isErrorLogMandatory: Boolean) {

        val progressLog = File(workingContext.externalCacheDir, FILE_PROGRESSLOG)
        val errorLog = File(workingContext.externalCacheDir, FILE_ERRORLOG)

        val backupScripts = workingContext.externalCacheDir.let {
            if (it != null) it.listFiles { f: File ->
                (f.name.startsWith(FILE_PREFIX_BACKUP_SCRIPT) || f.name.startsWith(FILE_PREFIX_RETRY_SCRIPT) || f.name.startsWith(FILE_PREFIX_TAR_CHECK))
                        && f.name.endsWith(".sh")
            }
            else emptyArray<File>()
        }

        val rawList = File(workingContext.externalCacheDir, FILE_RAW_LIST)

        if (isErrorLogMandatory && !errorLog.exists()) {
            AlertDialog.Builder(workingContext)
                    .setTitle(R.string.log_files_do_not_exist)
                    .setMessage(workingContext.getString(R.string.error_log_does_not_exist))
                    .setNegativeButton(android.R.string.cancel, null)
                    .show()
        }
        else if (errorLog.exists() || progressLog.exists() || backupScripts.isNotEmpty()){

            val eView = View.inflate(workingContext, R.layout.error_report_layout, null)

            eView.share_progress_checkbox.isChecked = progressLog.exists()
            eView.share_progress_checkbox.isEnabled = progressLog.exists()

            eView.share_script_checkbox.isChecked = backupScripts.isNotEmpty()
            eView.share_script_checkbox.isEnabled = backupScripts.isNotEmpty()

            eView.share_errors_checkbox.isChecked = errorLog.exists()
            eView.share_errors_checkbox.isEnabled = errorLog.exists() && !isErrorLogMandatory

            eView.share_rawList_checkbox.isChecked = rawList.exists()
            eView.share_rawList_checkbox.isEnabled = rawList.exists()

            eView.report_button_privacy_policy.setOnClickListener {
                workingContext.startActivity(Intent(workingContext, PrivacyPolicy::class.java))
            }

            eView.report_button_join_group.setOnClickListener {
                openWebLink(TG_LINK)
            }

            fun getUris(): ArrayList<Uri>{

                val uris = ArrayList<Uri>(0)
                try {

                    if (eView.share_errors_checkbox.isChecked) uris.add(getUri(errorLog))
                    if (eView.share_progress_checkbox.isChecked) uris.add(getUri(progressLog))
                    if (eView.share_script_checkbox.isChecked) for (f in backupScripts) uris.add(getUri(f))
                    if (eView.share_rawList_checkbox.isChecked) uris.add(getUri(rawList))

                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(workingContext, e.message.toString(), Toast.LENGTH_SHORT).show()
                }

                return uris
            }

            eView.report_button_old_email.setOnClickListener {
                sendIntent(getUris(), true)
            }

            var isTgClientInstalled = false
            for (i in TG_CLIENTS.indices){
                if (isPackageInstalled(TG_CLIENTS[i])){
                    isTgClientInstalled = true
                    break
                }
            }

            if (!isTgClientInstalled){
                eView.report_button_telegram.apply {
                    text = context.getString(R.string.install_tg)
                    setOnClickListener { playStoreLink(TG_CLIENTS[0]) }
                }
            }
            else {
                eView.report_button_telegram.apply {
                    text = context.getString(R.string.send_to_tg)
                    setOnClickListener { sendIntent(getUris()) }
                }
            }

            AlertDialog.Builder(workingContext).setView(eView).show()

        }
        else {

            val msg = workingContext.getString(R.string.progress_log_does_not_exist) + "\n" +
                    workingContext.getString(R.string.error_log_does_not_exist) + "\n" +
                    workingContext.getString(R.string.backup_script_does_not_exist) + "\n"

            AlertDialog.Builder(workingContext)
                    .setTitle(R.string.log_files_do_not_exist)
                    .setMessage(msg)
                    .setNegativeButton(android.R.string.cancel, null)
                    .show()
        }

    }

    var deviceSpecifications: String =
            "CPU_ABI: " + Build.SUPPORTED_ABIS[0] + "\n" +
                    "Brand: " + Build.BRAND + "\n" +
                    "Manufacturer: " + Build.MANUFACTURER + "\n" +
                    "Model: " + Build.MODEL + "\n" +
                    "Device: " + Build.DEVICE + "\n" +
                    "SDK: " + Build.VERSION.SDK_INT + "\n" +
                    "Board: " + Build.BOARD + "\n" +
                    "Hardware: " + Build.HARDWARE
        private set

    private fun getUri(file: File) =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                FileProvider.getUriForFile(workingContext, "migrate.provider", file)
            else Uri.fromFile(file)

    private fun sendIntent(uris: ArrayList<Uri>, isEmail: Boolean = false){
        Intent().run {

            action = Intent.ACTION_SEND_MULTIPLE
            type = "text/plain"
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION

            if (isEmail) {
                putExtra(Intent.EXTRA_EMAIL, arrayOf(REPORTING_EMAIL))
                putExtra(Intent.EXTRA_SUBJECT, "Log report for Migrate")
                putExtra(Intent.EXTRA_TEXT, deviceSpecifications)

                putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                workingContext.startActivity(Intent.createChooser(this, workingContext.getString(R.string.select_mail)))
            }
            else doBackgroundTask({

                tryIt {
                    val infoFile = File(workingContext.externalCacheDir, FILE_DEVICE_INFO)
                    BufferedWriter(FileWriter(infoFile)).run {
                        write(deviceSpecifications)
                        close()
                    }
                    uris.add(getUri(infoFile))
                }

            }, {
                this.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                workingContext.startActivity(Intent.createChooser(this, workingContext.getString(R.string.select_telegram)))
            })
        }
    }

    fun suEcho(): Array<Any> {
        val suRequest = Runtime.getRuntime().exec("su")

        val writer = BufferedWriter(OutputStreamWriter(suRequest.outputStream))

        //writer.write("pm grant " + context.getPackageName() + " android.permission.DUMP\n" );
        //writer.write("pm grant " + context.getPackageName() + " android.permission.PACKAGE_USAGE_STATS\n" );
        writer.write("exit\n")
        writer.flush()

        val errorReader = BufferedReader(InputStreamReader(suRequest.errorStream))
        val outputReader = BufferedReader(InputStreamReader(suRequest.inputStream))

        var line: String?
        var errorMessage = ""

        while (true) {
            line = outputReader.readLine()
            if (line != null) errorMessage = errorMessage + line + "\n"
            else break
        }
        errorMessage += "Error:\n\n"
        while (true) {
            line = errorReader.readLine()
            if (line != null) errorMessage = errorMessage + line + "\n"
            else break
        }

        suRequest.waitFor()
        return arrayOf(suRequest.exitValue() == 0, errorMessage)
    }

    fun getSdCardPaths(): Array<String> {
        val possibleSDCards = arrayListOf<String>()
        val storage = File("/storage/")
        if (storage.exists() && storage.canRead()) {
            val files = storage.listFiles { pathname ->
                (pathname.isDirectory && pathname.canRead()
                        && pathname.absolutePath != Environment.getExternalStorageDirectory().absolutePath)
            }
            for (f in files) {
                val sdDir = File("/mnt/media_rw/" + f.name)
                if (sdDir.exists() && sdDir.isDirectory && sdDir.canWrite())
                    possibleSDCards.add(sdDir.absolutePath)
            }
        }
        return possibleSDCards.toTypedArray()
    }

    fun showSdCardSupportDialog(): AlertDialog =
        AlertDialog.Builder(workingContext)
                .setView(View.inflate(workingContext, R.layout.learn_about_sd_card, null))
                .setPositiveButton(android.R.string.ok, null)
                .show()

    fun applyNamingCorrectionForShell(name: String) =
            name
                    .replace("`", "\\`")
                    .replace("!", "\\!")
                    .replace("#", "\\#")
                    .replace("$", "\\$")
                    .replace("&", "\\&")
                    .replace("*", "\\*")
                    .replace("(", "\\(")
                    .replace(")", "\\)")
                    .replace("[", "\\[")
                    .replace("]", "\\]")
                    .replace(">", "\\>")
                    .replace("<", "\\<")
                    .replace(" ", "\\ ")
                    .replace(":", "\\:")
                    .replace(";", "\\;")
                    .replace("\"", "\\\"")
                    .replace("\'", "\\\'")

    fun applyNamingCorrectionForDisplay(name: String) =
            name
                    .replace("\"", "'")
                    .replace("`", "'")
                    .replace("$", "")
                    .replace("\\s+".toRegex(), "_")
                    .replace("[^\\x20-\\x7E]".toRegex(), "")

    fun doBackgroundTask(job: () -> Unit, postJob: () -> Unit){
        class Class : AsyncTask<Any, Any, Any>(){
            override fun doInBackground(vararg params: Any?): Any {
                job()
                return 0
            }

            override fun onPostExecute(result: Any?) {
                super.onPostExecute(result)
                postJob()
            }
        }
        Class().execute()
    }

    fun forceCloseThis(){
        Runtime.getRuntime().exec("su").apply {
            BufferedWriter(OutputStreamWriter(this.outputStream)).run {
                this.write("am force-stop ${AppInstance.appContext.packageName}\n")
                this.write("exit\n")
                this.flush()
            }
        }

        val handler = Handler()
        handler.postDelayed({
            Toast.makeText(AppInstance.appContext, R.string.killing_programmatically, Toast.LENGTH_SHORT).show()
            android.os.Process.killProcess(android.os.Process.myPid())
        }, TIMEOUT_WAITING_TO_KILL)
    }
}