package balti.migrate.backupEngines

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import balti.filex.FileX
import balti.migrate.AppInstance.Companion.CACHE_DIR
import balti.migrate.R
import balti.migrate.simpleActivities.ProgressShowActivity
import balti.migrate.utilities.CommonToolsKotlin
import balti.migrate.utilities.CommonToolsKotlin.Companion.ACTION_BACKUP_CANCEL
import balti.migrate.utilities.CommonToolsKotlin.Companion.CHANNEL_BACKUP_CANCELLING
import balti.migrate.utilities.CommonToolsKotlin.Companion.CHANNEL_BACKUP_END
import balti.migrate.utilities.CommonToolsKotlin.Companion.CHANNEL_BACKUP_RUNNING
import balti.migrate.utilities.CommonToolsKotlin.Companion.FILE_ERRORLOG
import balti.migrate.utilities.CommonToolsKotlin.Companion.FILE_PROGRESSLOG
import balti.migrate.utilities.CommonToolsKotlin.Companion.NOTIFICATION_ID_ONGOING
import balti.module.baltitoolbox.functions.Misc.makeNotificationChannel
import balti.module.baltitoolbox.functions.Misc.tryIt
import java.io.BufferedWriter
import java.io.OutputStreamWriter

class BackupServiceKotlin_new: LifecycleService() {

    companion object {

        /**
         * String holding path to backup suited for FileX object.
         * If SAF storage, this will be blank.
         * If traditional storage, this will contain the full path.
         */
        var fileXDestination: String? = null

        /**
         * Flag to denote if backup is marked as cancelled.
         * All backup engines check this flag before proceeding.
         */
        var cancelBackup: Boolean = false

    }

    /**
     * Broadcast receiver to receive when "Cancel" Button is pressed from notification
     * or from [ProgressShowActivity].
     */
    private val cancelReceiver by lazy {
        object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                tryIt {
                    cancelBackup = true
                }
            }
        }
    }

    /**
     * Buffered writer to write to progressLog.txt
     */
    private var progressWriter: BufferedWriter? = null

    /**
     * Buffered writer to write to errorLog.txt
     */
    private var errorWriter: BufferedWriter? = null

    /**
     * Store last received title and subtask.
     * Do not write to log if same title and sub-task is received.
     */
    var lastTitle = ""
    var lastSubTask = ""

    private val commonTools by lazy { CommonToolsKotlin(this) }

    override fun onCreate() {
        super.onCreate()

        /**
         * Service ongoing notification.
         */
        val loadingNotification = NotificationCompat.Builder(this, CHANNEL_BACKUP_RUNNING)
            .setContentTitle(getString(R.string.loading))
            .setSmallIcon(R.drawable.ic_notification_icon)
            .build()

        /**
         * Initiate log writers.
         */
        tryIt {
            progressWriter = BufferedWriter(OutputStreamWriter(FileX.new(CACHE_DIR, FILE_PROGRESSLOG, true).outputStream()))
            errorWriter = BufferedWriter(OutputStreamWriter(FileX.new(CACHE_DIR, FILE_ERRORLOG, true).outputStream()))
        }

        /**
         * Create notification channels.
         */
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            makeNotificationChannel(CHANNEL_BACKUP_RUNNING, CHANNEL_BACKUP_RUNNING, NotificationManager.IMPORTANCE_LOW)
            makeNotificationChannel(CHANNEL_BACKUP_END, CHANNEL_BACKUP_END, NotificationManager.IMPORTANCE_HIGH)
            makeNotificationChannel(CHANNEL_BACKUP_CANCELLING, CHANNEL_BACKUP_CANCELLING, NotificationManager.IMPORTANCE_MIN)
        }

        /** Register cancel receivers */
        registerReceiver(cancelReceiver, IntentFilter(ACTION_BACKUP_CANCEL))
        commonTools.LBM?.registerReceiver(cancelReceiver, IntentFilter(ACTION_BACKUP_CANCEL))

        startForeground(NOTIFICATION_ID_ONGOING, loadingNotification)
    }

    override fun onDestroy() {
        super.onDestroy()

        fileXDestination = null

        tryIt { commonTools.LBM?.unregisterReceiver(cancelReceiver) }
        tryIt { unregisterReceiver(cancelReceiver) }

        tryIt { progressWriter?.close() }
        tryIt { errorWriter?.close() }
    }
}