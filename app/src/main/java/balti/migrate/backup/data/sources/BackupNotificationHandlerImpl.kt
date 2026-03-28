package balti.migrate.backup.data.sources

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import balti.migrate.R
import balti.migrate.backup.data.service.BackupService
import balti.migrate.common.data.model.NotificationInfo
import balti.migrate.common.utils.DeepLinkUtils
import balti.migrate.common.utils.convertToNotificationBuilder
import balti.migrate.common.utils.getCancelAction
import balti.migrate.common.utils.makeNotificationChannel
import baltiapps.migrate.domain.ACTION_CANCEL_BACKUP
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

        private const val NOTIFICATION_ID_BACKUP_ONGOING = 130
        private const val NOTIFICATION_ID_BACKUP_FINISHED = 131
        private const val NOTIFICATION_ID_BACKUP_CANCELLED = 132
        private const val NOTIFICATION_ID_BACKUP_FINISHED_WITH_ERRORS = 133
    }

    private val notificationManager by lazy {
        context.getSystemService(NotificationManager::class.java)
    }

    private val deepLinkUtils = DeepLinkUtils()

    private val pendingIntent by lazy {
        deepLinkUtils.getPendingIntent(DeepLinkUtils.MigrateUri.UriProgressBackup, context)
    }

    override fun setup() {
        Timber.i("Creating progress sample job")
        progressSamplerJob = Job()

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

    override fun getFinishedNotification(): NotificationInfo {
        return NotificationInfo(
            notificationId = NOTIFICATION_ID_BACKUP_FINISHED,
            notificationChannelId = CHANNEL_BACKUP_END_ID,
            icon = R.drawable.notification_icon_00,
            title = context.getString(R.string.backup_finished),
        )
    }

    override fun getCancelledNotification(): NotificationInfo {
        return NotificationInfo(
            notificationId = NOTIFICATION_ID_BACKUP_CANCELLED,
            notificationChannelId = CHANNEL_BACKUP_END_ID,
            icon = R.drawable.notification_icon_00,
            title = context.getString(R.string.backup_cancelled),
        )
    }

    override fun getFinishedWithErrorNotification(): NotificationInfo {
        return NotificationInfo(
            notificationId = NOTIFICATION_ID_BACKUP_FINISHED_WITH_ERRORS,
            notificationChannelId = CHANNEL_BACKUP_END_ID,
            icon = R.drawable.notification_icon_00,
            title = context.getString(R.string.backup_finished_with_errors),
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
            text = progress.displayText,
            progress = progressInt,
            maxProgress = 100,
            isIndeterminate = false,
            shouldShowProgress = true,
        )
    }

    override fun displayNotification(notification: NotificationInfo) {
        val notificationBuilder = notification.convertToNotificationBuilder(context).apply {
            setContentIntent(pendingIntent)

            if (notification.notificationId != NOTIFICATION_ID_BACKUP_ONGOING) return@apply

            setOngoing(true)
            addAction(
                notification.getCancelAction(context) {
                    Intent(context, BackupService::class.java).apply {
                        action = ACTION_CANCEL_BACKUP
                    }
                }
            )
        }

        notificationManager.notify(
            notification.notificationId,
            notificationBuilder.build(),
        )
    }
}