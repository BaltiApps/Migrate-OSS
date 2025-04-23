package balti.migrate.backup.data.sources

import android.app.NotificationManager
import android.content.Context
import balti.migrate.R
import balti.migrate.common.data.model.NotificationInfo
import balti.migrate.common.utils.NotificationUtils
import baltiapps.migrate.domain.common.model.Progress
import baltiapps.migrate.domain.common.repository.ProgressLogRepository
import baltiapps.migrate.domain.common.sources.ContextSource
import baltiapps.migrate.domain.common.sources.NotificationHandler
import kotlinx.coroutines.Job
import timber.log.Timber
import kotlin.math.roundToInt

class BackupNotificationHandlerImpl(
    private val context: Context,
    private val contextSource: ContextSource,
    private val progressLogRepository: ProgressLogRepository,
): NotificationHandler<NotificationInfo>() {

    companion object {
        private const val CHANNEL_BACKUP_END_ID = "backup_finished"
        private const val CHANNEL_BACKUP_END_DESC = "Backup finished notification"
        private const val CHANNEL_BACKUP_RUNNING_ID = "backup_running"
        private const val CHANNEL_BACKUP_RUNNING_DESC = "Backup running notification"
        private const val CHANNEL_BACKUP_CANCELLING_ID = "backup_cancelling"
        private const val CHANNEL_BACKUP_CANCELLING_DESC = "Cancelling current backup"

        private const val NOTIFICATION_ID_BACKUP_ONGOING = 130
        private const val NOTIFICATION_ID_BACKUP_COMPLETE = 131
        private const val NOTIFICATION_ID_BACKUP_CANCELLED = 132
    }

    private val notificationManager by lazy {
        context.getSystemService(NotificationManager::class.java)
    }

    private val notificationUtils = NotificationUtils()

    override fun setup() {
        Timber.i("Creating progress sample job")
        progressSamplerJob = Job()

        notificationUtils.run {
            notificationManager.makeNotificationChannel(
                channelId = CHANNEL_BACKUP_RUNNING_ID,
                channelDesc = CHANNEL_BACKUP_RUNNING_DESC,
                importance = NotificationManager.IMPORTANCE_LOW
            )
            notificationManager.makeNotificationChannel(
                channelId = CHANNEL_BACKUP_END_ID,
                channelDesc = CHANNEL_BACKUP_END_DESC,
                importance = NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.makeNotificationChannel(
                channelId = CHANNEL_BACKUP_CANCELLING_ID,
                channelDesc = CHANNEL_BACKUP_CANCELLING_DESC,
                importance = NotificationManager.IMPORTANCE_MIN
            )
        }
    }

    override fun getInitialNotification(): NotificationInfo {
        return NotificationInfo(
            notificationId = NOTIFICATION_ID_BACKUP_ONGOING,
            notificationChannelId = CHANNEL_BACKUP_RUNNING_ID,
            icon = R.drawable.notification_rotating_icon,
            title = context.getString(R.string.loading),
            shouldShowProgress = true,
            isIndeterminate = true,
        )
    }

    override suspend fun getLatestProgress(): Progress {
        return progressLogRepository.getLatestProgress()
    }

    override fun getProgressNotification(progress: Progress): NotificationInfo {
        val progressInt = (progress.percentage*100).roundToInt()
        val title = contextSource.getProgressTitle(progress.progressType)

        return (getInitialNotification()).copy(
            title = title,
            text = progress.logs,
            progress = progressInt,
            maxProgress = 100,
            isIndeterminate = false,
            shouldShowProgress = true,
        )
    }

    override fun displayNotification(notification: NotificationInfo) {
        notificationUtils.run {
            notificationManager.notify(
                notification.notificationId,
                notification.convertToNotificationBuilder(context).build(),
            )
        }
    }
}