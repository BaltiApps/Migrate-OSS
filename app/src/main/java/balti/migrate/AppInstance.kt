package balti.migrate

import android.app.ActivityManager
import android.app.Application
import android.app.NotificationManager
import android.content.Context
import balti.migrate.backupEngines.containers.ZipAppBatch
import balti.migrate.extraBackupsActivity.apps.containers.AppPacket
import balti.migrate.extraBackupsActivity.calls.containers.CallsDataPacketsKotlin
import balti.migrate.extraBackupsActivity.contacts.containers.ContactsDataPacketKotlin
import balti.migrate.extraBackupsActivity.sms.containers.SmsDataPacketKotlin
import balti.migrate.extraBackupsActivity.wifi.containers.WifiDataPacket
import balti.migrate.utilities.CommonToolsKotlin.Companion.PREF_MAX_BACKUP_SIZE
import balti.module.baltitoolbox.ToolboxHQ
import balti.module.baltitoolbox.functions.SharedPrefs.getPrefLong
import balti.module.baltitoolbox.functions.SharedPrefs.putPrefLong
import java.io.File


class AppInstance: Application() {

    companion object{
        lateinit var appContext: Context
        lateinit var notificationManager: NotificationManager
        var MAX_EFFECTIVE_ZIP_SIZE = 0L
        var MAX_WORKING_SIZE = 0L

        val appPackets = ArrayList<AppPacket>(0)
        val zipBatches = ArrayList<ZipAppBatch>(0)

        val contactsList = ArrayList<ContactsDataPacketKotlin>(0)
        val callsList = ArrayList<CallsDataPacketsKotlin>(0)
        val smsList = ArrayList<SmsDataPacketKotlin>(0)
        var dpiText : String? = null
        var keyboardText : String? = null
        var adbState : Int? = null
        var fontScale : Double? = null
        var wifiData : WifiDataPacket? = null

        var doBackupInstallers = false

        val RESERVED_SPACE = 6553000L          // this accounts for the helper apk and other scripts
    }

    private val MAX_TWRP_SIZE = 4194300000L
    private val DEVICE_RAM_SIZE : Long by lazy {
        val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        ActivityManager.MemoryInfo().let {
            activityManager.getMemoryInfo(it)
            it.totalMem
        }
    }

    override fun onCreate() {
        super.onCreate()
        appContext = this
        ToolboxHQ.init(this)
        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        MAX_EFFECTIVE_ZIP_SIZE = if (DEVICE_RAM_SIZE < MAX_TWRP_SIZE) DEVICE_RAM_SIZE else MAX_TWRP_SIZE

        MAX_WORKING_SIZE = getPrefLong(PREF_MAX_BACKUP_SIZE, MAX_EFFECTIVE_ZIP_SIZE).let {
            if (it > MAX_EFFECTIVE_ZIP_SIZE){
                putPrefLong(PREF_MAX_BACKUP_SIZE, MAX_EFFECTIVE_ZIP_SIZE)
                MAX_EFFECTIVE_ZIP_SIZE
            }
            else it
        }

        externalCacheDir?.run { File(this.absolutePath).mkdirs() }
    }

    fun refreshMaxSize() {
    }

}