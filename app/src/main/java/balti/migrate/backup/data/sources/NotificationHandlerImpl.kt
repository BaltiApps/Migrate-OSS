package balti.migrate.backup.data.sources

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import balti.migrate.R
import baltiapps.migrate.domain.backup.model.Progress
import baltiapps.migrate.domain.backup.sources.NotificationHandler
import baltiapps.migrate.domain.backup.repository.BackupProgressLogRepository
import baltiapps.migrate.domain.backup.sources.ContextSource
import kotlinx.coroutines.Job
import timber.log.Timber
import kotlin.math.roundToInt

class NotificationHandlerImpl(
    private val context: Context,
    private val contextSource: ContextSource,
    private val backupProgressLogRepository: BackupProgressLogRepository,
): NotificationHandler<NotificationCompat.Builder>() {

    companion object {
        const val CHANNEL_BACKUP_END_ID = "backup_finished"
        const val CHANNEL_BACKUP_END_DESC = "Backup finished notification"
        const val CHANNEL_BACKUP_RUNNING_ID = "backup_running"
        const val CHANNEL_BACKUP_RUNNING_DESC = "Backup running notification"
        const val CHANNEL_BACKUP_CANCELLING_ID = "backup_cancelling"
        const val CHANNEL_BACKUP_CANCELLING_DESC = "Cancelling current backup"

        const val NOTIFICATION_ID_BACKUP_ONGOING = 130
        const val NOTIFICATION_ID_BACKUP_COMPLETE = 131
        const val NOTIFICATION_ID_BACKUP_CANCELLED = 132
    }

    private val notificationManager by lazy {
        context.getSystemService(NotificationManager::class.java)
    }

    override fun setup() {
        Timber.i("Creating progress sample job")
        progressSamplerJob = Job()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            makeNotificationChannel(
                channelId = CHANNEL_BACKUP_RUNNING_ID,
                channelDesc = CHANNEL_BACKUP_RUNNING_DESC,
                importance = NotificationManager.IMPORTANCE_LOW
            )
            makeNotificationChannel(
                channelId = CHANNEL_BACKUP_END_ID,
                channelDesc = CHANNEL_BACKUP_END_DESC,
                importance = NotificationManager.IMPORTANCE_HIGH
            )
            makeNotificationChannel(
                channelId = CHANNEL_BACKUP_CANCELLING_ID,
                channelDesc = CHANNEL_BACKUP_CANCELLING_DESC,
                importance = NotificationManager.IMPORTANCE_MIN
            )
        }

        getInitialNotification()
    }

    override fun getInitialNotification(): NotificationCompat.Builder {
        val initialNotification =
            NotificationCompat.Builder(context, CHANNEL_BACKUP_RUNNING_ID)
                .setSmallIcon(R.drawable.notification_rotating_icon)
                .setContentTitle(context.getString(R.string.loading))
                .setProgress(0, 0, true)
                // https://stackoverflow.com/a/73074884/10967630
                .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
        currentNotification = initialNotification
        return initialNotification
    }

    override suspend fun getLatestProgress(): Progress {
        return backupProgressLogRepository.getLatestProgress()
    }

    override fun displayNotification(progress: Progress) {
        val progressInt = (progress.percentage*100).roundToInt()
//        Timber.i("Updating notification - $progressInt - $progress")
        val title = contextSource.getProgressTitle(progress.progressType)
        currentNotification?.apply {
            setOngoing(true)
            setContentTitle(title)
            setContentText(progress.logs)
            setProgress(100, progressInt, false)

            notificationManager.notify(NOTIFICATION_ID_BACKUP_ONGOING, build())
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun makeNotificationChannel(
        channelId: String,
        channelDesc: String,
        importance: Int,
    ) {
        notificationManager.createNotificationChannel(
            NotificationChannel(channelId, channelDesc, importance)
        )
    }
}