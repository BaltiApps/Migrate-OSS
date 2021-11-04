package balti.migrate.utilities

import android.app.Activity
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.widget.Toast
import balti.filex.FileX
import balti.migrate.AppInstance
import balti.migrate.R
import balti.migrate.simpleActivities.ReportLogs
import balti.migrate.storageSelector.StorageType
import balti.module.baltitoolbox.functions.Misc
import balti.module.baltitoolbox.functions.Misc.tryIt
import balti.module.baltitoolbox.functions.SharedPrefs.getPrefString
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter

class CommonToolsKotlin(val context: Context? = null) {

    companion object {

        val THIS_VERSION = 50
        val LAST_SUPPORTED_ANDROID_API = 30

        val IS_API_A11 : Boolean by lazy { Build.VERSION.SDK_INT >= Build.VERSION_CODES.R }
        val IS_API_A8 : Boolean by lazy { Build.VERSION.SDK_INT >= Build.VERSION_CODES.O }

        val FLAG_UPDATE_CURRENT_PENDING_INTENT by lazy {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            else PendingIntent.FLAG_UPDATE_CURRENT
        }

        val SU_INIT = if (IS_API_A11) "su --mount-master" else "su"

        val DEBUG_TAG = "migrate_tag"

        val NOTIFICATION_ID_ONGOING = 129
        val NOTIFICATION_ID_FINISHED = 130
        val NOTIFICATION_ID_CANCELLING = 131

        val PENDING_INTENT_REQUEST_ID = 913
        val PENDING_INTENT_BACKUP_CANCEL_ID = 912

        val DEFAULT_INTERNAL_STORAGE_DIR = "/sdcard/Migrate"
        val MIGRATE_CACHE_DEFAULT = "/data/local/tmp/migrate_cache"
        val PRIVATE_BACKUP_CACHE_NAME = "backup_cache"

        val DIR_MANUAL_CONFIGS = "manualConfigs"
        val DIR_APP_AUX_FILES = "app_aux_files"
        val DIR_APK_FILES_SIZES = "apk_files_sizes"
        val FILE_MIGRATE_CACHE_MANUAL = "MIGRATE_CACHE_MANUAL"
        val FILE_SYSTEM_MANUAL = "SYSTEM_MANUAL"
        val FILE_BUILDPROP_MANUAL = "BUILDPROP_MANUAL"

        val FILE_PROGRESSLOG = "progressLog.txt"
        val FILE_ERRORLOG = "errorLog.txt"
        val FILE_DEVICE_INFO = "device_info.txt"
        val FILE_PREFIX_BACKUP_SCRIPT = "the_backup_script"
        val FILE_PREFIX_RETRY_SCRIPT = "retry_script"
        val FILE_PREFIX_TAR_CHECK = "tar_check"

        val FILE_SPLIT_APK_NAMES_LIST = "split_apk_names_list.txt"

        val FILE_PREFIX_MOVE_TO_CONTAINER_SCRIPT = "move_to_containers"

        val FILE_ZIP_NAME_EXTRAS = "Extras"

        /**
         * Created in [balti.migrate.backupEngines.engines.MakeZipBatch.createMoveScript].
         */
        val FILE_FILE_LIST = "fileList.txt"

        /**
         * Created in [balti.migrate.backupEngines.engines.UpdaterScriptMakerEngine.createRawList].
         */
        val FILE_RAW_LIST = "rawList.txt"

        /**
         * Created in [balti.migrate.backupEngines.engines.UpdaterScriptMakerEngine.makePackageData].
         */
        val FILE_PACKAGE_DATA = "package-data.txt"

        /** Created in main activity. */
        val FILE_MESSAGES = "messages.txt"

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

        val EXTRA_IS_ALL_APP_SELECTED = "isAllAppsSelected"
        val EXTRA_BACKUP_NAME = "backupName"
        val EXTRA_CANONICAL_DESTINATION = "canonicalDestination"
        val EXTRA_FILEX_DESTINATION = "fileXDestination"
        val EXTRA_ACTUAL_DESTINATION = "actualDestination"
        val EXTRA_ERRORS = "errors"
        val EXTRA_FINISHED_ZIP_PATHS = "finishedZipPaths"
        val EXTRA_WARNINGS = "warnings"
        val EXTRA_TOTAL_TIME = "total_time"
        val EXTRA_IS_CANCELLED = "isCancelled"
        val EXTRA_FLASHER_ONLY = "flasherOnly"
        val EXTRA_PROGRESS_ACTIVITY_FROM_FINISHED_NOTIFICATION = "PROGRESS_ACTIVITY_FROM_FINISHED_NOTIFICATION"

        val EXTRA_IS_ERROR_LOG_MANDATORY = "isErrorLogMandatory"

        val ERR_ZIP_TRY_CATCH = "ZIP_TRY_CATCH"
        val ERR_VERIFICATION_TRY_CATCH = "VERIFICATION_TRY_CATCH"
        val ERR_CORRECTION_SHELL = "CORRECTION_SHELL"
        val ERR_CORRECTION_SUPPRESSED = "CORRECTION_SUPPRESSED"
        val ERR_CORRECTION_TRY_CATCH = "CORRECTION_TRY_CATCH"
        val ERR_TAR_SHELL = "TAR_ERR"
        val ERR_TAR_SUPPRESSED = "TAR_SUPPRESSED"
        val ERR_TAR_CHECK_TRY_CATCH = "TAR_TRY_CATCH"
        val ERR_APK_CHECK_TRY_CATCH = "APK_VERI_TRY_CATCH"
        val ERR_SCRIPT_MAKING_TRY_CATCH = "SCRIPT_MAKING_TRY_CATCH"
        val ERR_AUX_MOVING_TRY_CATCH = "AUX_MOVING_TRY_CATCH"
        val ERR_CORRECTION_AUX_MOVING_TRY_CATCH = "CORRECTION_AUX_MOVING_TRY_CATCH"
        val ERR_AUX_MOVING_SU = "ERR_AUX_MOVING_SU"
        val ERR_CORRECTION_AUX_MOVING_SU = "CORRECTION_AUX_MOVING_SU"
        val ERR_CREATING_MTD = "ERR_CREATING_MTD"
        val ERR_APP_BACKUP_SHELL = "RUN"
        val ERR_APP_BACKUP_SUPPRESSED = "RUN_SUPPRESSED"
        val ERR_APP_BACKUP_TRY_CATCH = "RUN_TRY_CATCH"
        val ERR_ZIP_VERIFICATION_TRY_CATCH = "ZIP_VERI_TRY_CATCH"
        val ERR_ZIP_ITEM_UNAVAILABLE = "ZIP_ITEM_UNAVAILABLE"
        val ERR_ZIP_FL_ITEM_UNAVAILABLE = "ZIP_FL_ITEM_UNAVAILABLE"
        val ERR_ZIP_FL_UNAVAILABLE = "ZIP_FILELIST_UNAVAILABLE"
        val ERR_ZIP_TOO_BIG = "ZIP_TOO_BIG"
        val ERR_ZIP_SIZE_ZERO = "ZIP_SIZE_ZERO"
        val ERR_CONTACTS_TRY_CATCH = "CONTACTS_TRY_CATCH"
        val ERR_CALLS_WRITE = "CALLS_WRITE"
        val ERR_CALLS_WRITE_TO_ACTUAL = "CALLS_WRITE_TO_ACTUAL"
        val ERR_CALLS_TRY_CATCH = "CALLS_TRY_CATCH"
        val ERR_CALLS_VERIFY = "CALLS_VERIFY"
        val ERR_CALLS_VERIFY_TRY_CATCH = "CALLS_VERIFY_TRY_CATCH"
        val ERR_SMS_WRITE = "SMS_WRITE"
        val ERR_SMS_WRITE_TO_ACTUAL = "SMS_WRITE_TO_ACTUAL"
        val ERR_SMS_TRY_CATCH = "SMS_TRY_CATCH"
        val ERR_SMS_VERIFY = "SMS_VERIFY"
        val ERR_SMS_VERIFY_TRY_CATCH = "SMS_VERIFY_TRY_CATCH"
        val ERR_WIFI_TRY_CATCH = "WIFI_TRY_CATCH"
        val ERR_SETTINGS_TRY_CATCH = "SETTINGS_TRY_CATCH"
        val ERR_TESTING_ERROR = "SYSTEM_TESTING_ERROR"
        val ERR_TESTING_TRY_CATCH = "SYSTEM_TESTING_TRY_CATCH"
        val ERR_UPDATER_TRY_CATCH = "UPDATER_TRY_CATCH"
        val ERR_UPDATER_MOVE_AUX = "UPDATER_MOVE_AUX"
        val ERR_UPDATER_EXTRACT = "UPDATER_EXTRACT"
        val ERR_UPDATER_CONFIG_FILE = "UPDATER_CONFIG_FILE"
        val ERR_WRITING_RAW_LIST = "ERR_WRITING_RAW_LIST"
        val ERR_WRITING_RAW_LIST_CATCH = "ERR_WRITING_RAW_LIST_CATCH"
        val ERR_ZIP_PACKET_MAKING = "ZIP_PACKET_MAKING"
        val ERR_MAKING_APP_ZIP_PACKET = "MAKING_APP_ZIP_PACKET"
        val ERR_MAKING_EXTRA_ZIP_PACKET = "MAKING_EXTRA_ZIP_PACKET"
        val ERR_ZIP_BATCHING = "ZIP_BATCHING"
        val ERR_ZIP_ADDING_EXTRAS = "ZIP_ADDING_EXTRAS"
        val ERR_ZIP_ENGINE_INIT = "ZIP_ENGINE_INIT"
        val ERR_MOVING = "MOVING_FILES"
        val ERR_MOVING_ROOT = "MOVING_FILES_SU"
        val ERR_MOVE_SCRIPT = "MOVE_SCRIPT"
        val ERR_MOVE_TRY_CATCH = "MOVE_EXECUTE_TRY_CATCH"
        val ERR_READING_APK_SIZE = "ERR_READING_APK_SIZE"
        val ERR_CASTING_APK_SIZE = "ERR_CASTING_APK_SIZE"
        val ERR_APK_SIZE_INDEX_NOT_FOUND = "ERR_APK_SIZE_INDEX_NOT_FOUND"
        val ERR_NO_RAW_LIST = "NO_RAW_LIST"
        val ERR_PARSING_RAW_LIST = "PARSING_RAW_LIST"
        val ERR_ZIP_FILELIST_UNAVAILABLE = "NO_FILELIST_IN_ZIP_VERIFICATION"
        val ERR_ZIP_FILELIST_ITEM_UNAVAILABLE = "FILELIST_ITEM_UNAVAILABLE"

        val ERR_BACKUP_SERVICE_ERROR = "BACKUP_SERVICE"
        val ERR_CONDITIONAL_TASK = "RUN_CONDITIONAL_TASK"
        val ERR_ON_COMPLETE_TASK = "ON_COMPLETE_TASK"
        val ERR_BACKUP_SERVICE_INIT = "BACKUP_SERVICE_INIT"
        val ERR_GENERIC_BACKUP_ENGINE = "ERR_GENERIC_BACKUP_ENGINE"

        val ALL_SUPPRESSED_ERRORS = arrayOf(ERR_APP_BACKUP_SUPPRESSED, ERR_CORRECTION_SUPPRESSED, ERR_TAR_SUPPRESSED)

        val WARNING_ZIP_BATCH = "ZIP_BATCH_WARNING"
        val WARNING_CALLS = "CALL_VERIFY_WARNING"
        val WARNING_SMS = "SMS_VERIFY_WARNING"
        val WARNING_ZIP_FILELIST_VERIFICATION = "ZIP_FILELIST"
        val ERR_FILE_LIST_COPY = "FILE_LIST_COPY"
        val WARNING_CASTING_APK_SIZE = "CASTING_APK_SIZE_FAILED"
        val WARNING_APK_SIZE_INFO_WRONG = "APK_INFO_INCORRECT"

        val ALL_WARNINGS = arrayOf(
            WARNING_ZIP_BATCH,
            WARNING_CALLS,
            WARNING_SMS,
            WARNING_ZIP_FILELIST_VERIFICATION,
            WARNING_CASTING_APK_SIZE,
            WARNING_APK_SIZE_INFO_WRONG,
        )

        val LOG_CORRECTION_NEEDED = "CORRECTION_NEEDED"

        val PACKAGE_NAME_PLAY_STORE = "com.android.vending"
        val PACKAGE_NAME_FDROID = "org.fdroid.fdroid.privileged"
        val PACKAGE_NAME_AURORA = "com.aurora.services"
        val PACKAGE_NAMES_KNOWN = arrayOf(PACKAGE_NAME_PLAY_STORE, PACKAGE_NAME_FDROID, PACKAGE_NAME_AURORA)
        val PACKAGE_NAMES_PACKAGE_INSTALLER = arrayOf("com.google.android.packageinstaller")

        val QUALIFIED_PACKAGE_INSTALLERS = HashMap<String, String>().apply {
            PACKAGE_NAME_AURORA.run { Misc.getAppName(this).let { if (it.isNotBlank()) put(this,it) } }
            PACKAGE_NAME_PLAY_STORE.run { Misc.getAppName(this).let { if (it.isNotBlank()) put(this,it) } }
            PACKAGE_NAME_FDROID.run { Misc.getAppName(this).let { if (it.isNotBlank()) put(this,it) } }
        }

        val PREF_USE_FILEX11 = "use_filex"
        val PREF_FIRST_STORAGE_REQUEST = "first_storage_request"

        val PREF_FILE_APPS = "apps"
        val PREF_FIRST_RUN = "firstRun"
        val PREF_VERSION_CURRENT = "version"
        val PREF_ANDROID_VERSION_WARNING = "android_version_warning"
        val PREF_DEFAULT_BACKUP_PATH = "defaultBackupPath"
        val PREF_STORAGE_TYPE = "storageType"
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
        val PREF_RETRY_SYSTEM_CHECK = "retry_system_check"
        //val PREF_SEPARATE_EXTRAS_BACKUP = "separate_extras"
        //val PREF_FORCE_SEPARATE_EXTRAS_BACKUP = "force_separate_extras"
        val PREF_DELETE_ERROR_BACKUP = "delete_backup_on_error"
        val PREF_USE_SU_FOR_KEYBOARD = "use_su_for_keyboard"
        val PREF_ZIP_VERIFICATION = "do_zip_verification"
        val PREF_FILELIST_IN_ZIP_VERIFICATION = "fileList_in__zip_verification"
        val PREF_SHOW_BACKUP_SUMMARY = "showBackupSummary"
        val PREF_USE_SHELL_TO_SPEED_UP = "useShellToSpeedUp"
        val PREF_SEPARATE_EXTRAS_FOR_FLASHER_ONLY = "separate_extras_for_flasher_only"
        val PREF_SEPARATE_EXTRAS_FOR_SMALL_BACKUP = "separate_extras_for_small_backup"

        val PREF_IGNORE_APP_CACHE = "ignore_app_cache"

        val PREF_MANUAL_MIGRATE_CACHE = "manual_migrate_cache"
        val PREF_MANUAL_SYSTEM = "manual_system"
        val PREF_MANUAL_BUILDPROP = "manual_buildProp"

        val PREF_BACKUP_SMS = "last_sms_backup_state"
        val PREF_BACKUP_CALLS = "last_calls_backup_state"
        val PREF_BACKUP_INSTALLERS = "last_installers_backup_state"
        val PREF_BACKUP_ADB = "last_adb_state"
        val PREF_BACKUP_FONTSCALE = "last_fontscale_state"
        val PREF_BACKUP_KEYBOARD = "last_keyboard_state"
        val PREF_BACKUP_DPI = "last_dpi_state"
        val PREF_SHOW_MANDATORY_FLASHER_WARNING = "flasher_only_warning"
        val PREF_USE_FLASHER_ONLY = "use_flasher_only"

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

        val MESSAGE_BOARD_URL = "https://gitlab.com/SayantanRC/update-files/-/raw/master/migrate_message_board.txt"

        val MESSAGE_FIELD_LAST_UPDATE_NO = "message_update_no"
        val MESSAGE_FIELD_MESSAGE_ARRAY = "messages"
        val MESSAGE_FIELD_MESSAGE_NO = "message_no"
        val MESSAGE_FIELD_MESSAGE_TITLE = "message_title"
        val MESSAGE_FIELD_MESSAGE_BODY = "message_body"
        val MESSAGE_FIELD_DATE = "date"
        val MESSAGE_LINK = "message_link"

        val PREF_LAST_MESSAGE_LEVEL = "last_message_level"
        val PREF_LAST_MESSAGE_SNACK_LEVEL = "last_message_snack_level"

        val EXTRA_MESSAGE_CONTENT = "message_content"

        val MESSAGE_ACTIVITY_CODE = 6666

        val PACKAGE_MIGRATE_FLASHER = "balti.migrate.flasher"

        fun isDeletable(f: FileX): Boolean {
            val d = getPrefString(PREF_DEFAULT_BACKUP_PATH, DEFAULT_INTERNAL_STORAGE_DIR)
            val parentPath = FileX.new(d).canonicalPath
            return f.canonicalPath.startsWith(parentPath)
        }
    }

    var LBM: androidx.localbroadcastmanager.content.LocalBroadcastManager? = null
    private val workingContext by lazy { context ?: AppInstance.appContext }

    init {
        if (context != null && workingContext is Activity || workingContext is Service)
            LBM = androidx.localbroadcastmanager.content.LocalBroadcastManager.getInstance(workingContext)
    }

    fun reportLogs(isErrorLogMandatory: Boolean) {
        val reportLogsIntent = Intent(context, ReportLogs::class.java).apply {
            putExtra(EXTRA_IS_ERROR_LOG_MANDATORY, isErrorLogMandatory)
        }
        context?.startActivity(reportLogsIntent)
    }

    val deviceSpecifications: String =
            "CPU_ABI: " + Build.SUPPORTED_ABIS[0] + "\n" +
                    "Brand: " + Build.BRAND + "\n" +
                    "Manufacturer: " + Build.MANUFACTURER + "\n" +
                    "Model: " + Build.MODEL + "\n" +
                    "Device: " + Build.DEVICE + "\n" +
                    "SDK: " + Build.VERSION.SDK_INT + "\n" +
                    "Board: " + Build.BOARD + "\n" +
                    "Hardware: " + Build.HARDWARE

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

    fun isStorageValid(): Boolean {
        val storageType = getPrefString(PREF_STORAGE_TYPE, StorageType.CONVENTIONAL.value)
        val storageLocation = getPrefString(PREF_DEFAULT_BACKUP_PATH, DEFAULT_INTERNAL_STORAGE_DIR)
        val isTraditional = storageType in arrayOf(StorageType.CONVENTIONAL.value, StorageType.ALL_FILES_STORAGE.value)
        return FileX.new(if (isTraditional) storageLocation else "/", isTraditional).run {
            tryIt { mkdirs() }
            canWrite() && isDirectory
        }
    }

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

    fun forceCloseThis() {
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