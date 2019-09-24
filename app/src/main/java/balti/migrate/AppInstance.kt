package balti.migrate

import android.app.ActivityManager
import android.app.Application
import android.app.NotificationManager
import android.content.Context
import android.content.SharedPreferences
import balti.migrate.utilities.CommonToolKotlin.Companion.FILE_MAIN_PREF
import balti.migrate.utilities.CommonToolKotlin.Companion.PREF_MAX_BACKUP_SIZE
import java.io.File


class AppInstance: Application() {

    companion object{
        lateinit var appContext: Context
        lateinit var sharedPrefs: SharedPreferences
        lateinit var notificationManager: NotificationManager
        var MAX_CUSTOM_ZIP_SIZE = 0L
        var MAX_WORKING_SIZE = 0L
    }

    private val MAX_TWRP_SIZE = 4194300L
    private val DEVICE_RAM_SIZE : Long by lazy {
        val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        ActivityManager.MemoryInfo().let {
            activityManager.getMemoryInfo(it)
            it.totalMem / 1024
        }
    }

    override fun onCreate() {
        super.onCreate()
        appContext = this
        sharedPrefs = getSharedPreferences(FILE_MAIN_PREF, Context.MODE_PRIVATE)
        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        MAX_CUSTOM_ZIP_SIZE = if (DEVICE_RAM_SIZE < MAX_TWRP_SIZE) DEVICE_RAM_SIZE else MAX_TWRP_SIZE

        MAX_WORKING_SIZE = sharedPrefs.getLong(PREF_MAX_BACKUP_SIZE, MAX_CUSTOM_ZIP_SIZE)

        File(externalCacheDir.absolutePath).mkdirs()
    }

}