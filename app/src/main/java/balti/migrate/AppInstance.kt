package balti.migrate

import android.app.ActivityManager
import android.app.Application
import android.app.NotificationManager
import android.content.Context
import balti.filex.FileX
import balti.filex.FileXInit
import balti.migrate.backupActivity.containers.BackupDataPacketKotlin
import balti.migrate.backupEngines.containers.ZipAppBatch
import balti.migrate.extraBackupsActivity.apps.containers.AppPacket
import balti.migrate.extraBackupsActivity.calls.containers.CallsDataPacketsKotlin
import balti.migrate.extraBackupsActivity.engines.contacts.containers.ContactsDataPacketKotlin
import balti.migrate.extraBackupsActivity.sms.containers.SmsDataPacketKotlin
import balti.migrate.extraBackupsActivity.wifi.containers.WifiDataPacket
import balti.migrate.utilities.CommonToolsKotlin.Companion.PREF_MAX_BACKUP_SIZE
import balti.migrate.utilities.CommonToolsKotlin.Companion.PREF_USE_FILEX11
import balti.migrate.utilities.CommonToolsKotlin.Companion.PRIVATE_BACKUP_CACHE_NAME
import balti.module.baltitoolbox.ToolboxHQ
import balti.module.baltitoolbox.functions.Misc.tryIt
import balti.module.baltitoolbox.functions.SharedPrefs.getPrefBoolean
import balti.module.baltitoolbox.functions.SharedPrefs.getPrefLong
import balti.module.baltitoolbox.functions.SharedPrefs.putPrefLong

/**
 * A class used for storing some data and some stuff to be available throughout the app.
 */
class AppInstance: Application() {

    companion object{
        /**
         * Stores the app context to be used anywhere in the project. This is initialised in [onCreate].
         * This is useful in classes which do not have a context readily available, for example to display a Toast message.
         */
        lateinit var appContext: Context

        /**
         * A [NotificationManager] object to be used anywhere in the project. Initialised in [onCreate].
         */
        lateinit var notificationManager: NotificationManager

        /**
         * This is the maximum zip size possible on the current device.
         * For a TWRP flashable zip, the maximum is 4GB or the device RAM, whichever is lower.
         * This variable is initialised in [onCreate], such that it is equal to [MAX_TWRP_SIZE] (roughly 4GB)
         * or the value from [DEVICE_RAM_SIZE], whichever is lower.
         *
         * The value is set in [refreshMaxZipSize].
         *
         * Note: This is not the final maximum zip size. The final maximum is considered from [MAX_WORKING_SIZE].
         */
        var MAX_EFFECTIVE_ZIP_SIZE = 0L

        /**
         * This is the maximum zip size value selected by the user. It can be adjusted from the preferences.
         * The maximum value is [MAX_EFFECTIVE_ZIP_SIZE].
         *
         * The value is set in [refreshMaxZipSize].
         *
         * This is the FINAL maximum zip size. Even if [MAX_EFFECTIVE_ZIP_SIZE] is (say) 3GB,
         * if user selects (say) 1GB in preference, then this value will correspond to 1GB which will finally be considered while creating zips.
         *
         * @see balti.migrate.preferences.subPreferences.ZipMaxSizePref
         */
        var MAX_WORKING_SIZE = 0L

        /**
         * A directory inside app's cache directory.
         * This location is used to store various files like backup logs and other temp files during the backup process.
         *
         * Actual path is: /data/data/balti.migrate/cache/backup_cache/
         */
        val CACHE_DIR: String by lazy {
            appContext.cacheDir.run {
                FileX.new(canonicalPath, PRIVATE_BACKUP_CACHE_NAME, true).let {
                    it.mkdirs()
                    it.canonicalPath
                    // /data/data/balti.migrate/cache/backup_cache/
                }
            }
        }

        /**
         * Central location store all AppPacket.
         * @see balti.migrate.extraBackupsActivity.apps.containers.AppPacket
         * @see appBackupDataPackets
         */
        val appPackets = ArrayList<AppPacket>(0)

        /**
         * Central location to store all zip batches.
         * @see balti.migrate.backupEngines.containers.ZipAppBatch
         */
        val zipBatches = ArrayList<ZipAppBatch>(0)

        /**
         * Central location to store all contact related info for backup.
         * @see balti.migrate.extraBackupsActivity.contacts.containers.ContactsDataPacketKotlin
         */
        val contactsList = ArrayList<ContactsDataPacketKotlin>(0)

        /**
         * Central location to store all call log related info for backup.
         * @see balti.migrate.extraBackupsActivity.calls.containers.CallsDataPacketsKotlin
         */
        val callsList = ArrayList<CallsDataPacketsKotlin>(0)

        /**
         * Central location to store all SMS related info for backup.
         * @see balti.migrate.extraBackupsActivity.sms.containers.SmsDataPacketKotlin
         */
        val smsList = ArrayList<SmsDataPacketKotlin>(0)

        /**
         * Central location to store all app related info.
         * Difference with [appPackets] is that this is used mainly in to display app list to users to select the apps for backup,
         * [appPackets] is used in the actual backup engines and contain a lot more data related to the app package name, base apk path etc.
         * This variable however, stores limited data about the options (APK, data, permission) the user has selected to backup.
         *
         * @see balti.migrate.backupActivity.containers.BackupDataPacketKotlin
         */
        val appBackupDataPackets by lazy { ArrayList<BackupDataPacketKotlin>(0) }

        /**
         * A filtered list from [appBackupDataPackets].
         * This contains only the packets which either have the option selected for APK, Data or permission.
         * All other packets which do not have any option selected are discarded.
         * This list is created in [balti.migrate.backupActivity.BackupActivityKotlin.filterApps]
         */
        val selectedBackupDataPackets by lazy { ArrayList<BackupDataPacketKotlin>(0) }

        /**
         * This stores the detected screen density.
         * @see balti.migrate.extraBackupsActivity.dpi.ReadDpiKotlin
         */
        var dpiText : String? = null

        /**
         * This stores the detected keyboard. The text contains the keyboard app package name and some other info.
         * @see balti.migrate.extraBackupsActivity.keyboard.LoadKeyboardForSelection
         */
        var keyboardText : String? = null

        /**
         * This stores the detected ADB debugging state. Valid values are 0 or 1. Any other value is ignored during restoration process.
         * @see balti.migrate.extraBackupsActivity.adb.ReadAdbKotlin
         */
        var adbState : Int? = null

        /**
         * This stores the font scale. It is a [Double] value returned by the system.
         * @see balti.migrate.extraBackupsActivity.fontScale.ReadFontScaleKotlin
         */
        var fontScale : Double? = null

        /**
         * This stores the contents of the wifi AP file under [balti.migrate.utilities.CommonToolsKotlin.WIFI_FILE_PATH]
         * @see balti.migrate.extraBackupsActivity.wifi.ReadWifiKotlin
         * @see balti.migrate.extraBackupsActivity.wifi.containers.WifiDataPacket
         */
        var wifiData : WifiDataPacket? = null

        /**
         * A boolean to store if the user has chosen to backup the app installers of apps.
         * @see balti.migrate.extraBackupsActivity.installer.LoadInstallersForSelection
         */
        var doBackupInstallers = false

        /**
         * This is an amount subtracted from [MAX_WORKING_SIZE].
         * This accounts for the helper apk and other scripts in the zip,
         * so that the overall zip size still remains under the [MAX_EFFECTIVE_ZIP_SIZE] limit.
         */
        val RESERVED_SPACE = 6553000L          // this accounts for the helper apk and other scripts
    }

    /**
     * Maximum allowed zip size by TWRP. This is close to 4GB.
     */
    private val MAX_TWRP_SIZE = 4194300000L

    /**
     * The current device RAM size. This is factored in while calculation [MAX_WORKING_SIZE] in [refreshMaxZipSize].
     */
    private val DEVICE_RAM_SIZE : Long by lazy {
        val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        ActivityManager.MemoryInfo().let {
            activityManager.getMemoryInfo(it)
            it.totalMem
        }
    }

    override fun onCreate() {
        super.onCreate()

        // initialize appContext and notificationManager
        appContext = this
        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        // Library initialization
        ToolboxHQ.init(this)
        FileXInit(this, !getPrefBoolean(PREF_USE_FILEX11, true))

        // set the value of MAX_WORKING_SIZE
        refreshMaxZipSize()

        // create externalCacheDir. This is probably legacy code and not used in version 5.0+ of the app.
        tryIt {
            externalCacheDir?.run { FileX.new(this.absolutePath, true).mkdirs() }
        }
    }

    /**
     * This method calculates the maximum zip size for TWRP flashable backups.
     * @see MAX_EFFECTIVE_ZIP_SIZE
     * @see MAX_WORKING_SIZE
     */
    fun refreshMaxZipSize() {
        // calculate maximum allowed zip size for this device
        MAX_EFFECTIVE_ZIP_SIZE = if (DEVICE_RAM_SIZE < MAX_TWRP_SIZE) DEVICE_RAM_SIZE else MAX_TWRP_SIZE

        // set the maximum zip size selected by the user from shared preference.
        MAX_WORKING_SIZE = getPrefLong(PREF_MAX_BACKUP_SIZE, MAX_EFFECTIVE_ZIP_SIZE).let {
            if (it > MAX_EFFECTIVE_ZIP_SIZE){
                putPrefLong(PREF_MAX_BACKUP_SIZE, MAX_EFFECTIVE_ZIP_SIZE)
                MAX_EFFECTIVE_ZIP_SIZE
            }
            else it
        }
    }

}