package balti.migrate.restore.data.sources

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import balti.migrate.R
import balti.migrate.common.data.model.NotificationInfo
import balti.migrate.common.utils.DeepLinkUtils
import balti.migrate.common.utils.makeNotificationChannel
import balti.migrate.common.utils.convertToNotificationBuilder
import balti.migrate.common.utils.getCancelAction
import balti.migrate.restore.data.service.RestoreService
import baltiapps.migrate.domain.ACTION_CANCEL_RESTORE
import baltiapps.migrate.domain.PermissionConstants
import baltiapps.migrate.domain.common.model.Progress
import baltiapps.migrate.domain.common.repository.ProgressLogRepository
import baltiapps.migrate.domain.common.sources.ContextSource
import baltiapps.migrate.domain.common.sources.NotificationHandler
import kotlinx.coroutines.Job
import timber.log.Timber
import kotlin.math.roundToInt

class RestoreNotificationHandlerImpl(
    private val context: Context,
    private val contextSource: ContextSource,
    private val progressLogRepository: ProgressLogRepository,
): NotificationHandler<NotificationInfo>() {

    companion object {
        private const val CHANNEL_RESTORE_END_ID = "restore_finished"
        private const val CHANNEL_RESTORE_END_DESC = "Restore finished notification"
        private const val CHANNEL_RESTORE_RUNNING_ID = "restore_running"
        private const val CHANNEL_RESTORE_RUNNING_DESC = "Restore running notification"

        private const val NOTIFICATION_ID_RESTORE_ONGOING = 230
        private const val NOTIFICATION_ID_RESTORE_COMPLETE = 231
        private const val NOTIFICATION_ID_RESTORE_CANCELLED = 232
        private const val NOTIFICATION_ID_RESTORE_FINISHED_WITH_ERRORS = 233
    }

    private val notificationManager by lazy {
        context.getSystemService(NotificationManager::class.java)
    }

    private val deepLinkUtils = DeepLinkUtils()

    private val pendingIntent by lazy {
        deepLinkUtils.getPendingIntent(DeepLinkUtils.MigrateUri.UriProgressRestore, context)
    }

    override fun setup() {
        Timber.i("Creating progress sample job")
        progressSamplerJob = Job()

        notificationManager.makeNotificationChannel(
            channelId = CHANNEL_RESTORE_RUNNING_ID,
            channelDesc = CHANNEL_RESTORE_RUNNING_DESC,
            importance = NotificationManager.IMPORTANCE_LOW
        )
        notificationManager.makeNotificationChannel(
            channelId = CHANNEL_RESTORE_END_ID,
            channelDesc = CHANNEL_RESTORE_END_DESC,
            importance = NotificationManager.IMPORTANCE_HIGH
        )
    }

    private fun isDefaultSmsApp(): Boolean {
        return contextSource.checkPermission(PermissionConstants.DEFAULT_SMS_APP)
    }

    private fun notificationContentTextOnFinish(): String? {
        return if (isDefaultSmsApp()) context.getString(R.string.please_change_sms_app) else null
    }

    override fun getInitialNotification(): NotificationInfo {
        return NotificationInfo(
            notificationId = NOTIFICATION_ID_RESTORE_ONGOING,
            notificationChannelId = CHANNEL_RESTORE_RUNNING_ID,
            icon = R.drawable.notification_rotating_icon,
            title = context.getString(R.string.loading),
            shouldShowProgress = true,
            isIndeterminate = true,
        )
    }

    override fun getFinishedNotification(): NotificationInfo {
        return NotificationInfo(
            notificationId = NOTIFICATION_ID_RESTORE_COMPLETE,
            notificationChannelId = CHANNEL_RESTORE_END_ID,
            icon = R.drawable.notification_icon_00,
            title = context.getString(R.string.restore_finished),
            text = notificationContentTextOnFinish(),
        )
    }

    override fun getCancelledNotification(): NotificationInfo {
        return NotificationInfo(
            notificationId = NOTIFICATION_ID_RESTORE_CANCELLED,
            notificationChannelId = CHANNEL_RESTORE_END_ID,
            icon = R.drawable.notification_icon_00,
            title = context.getString(R.string.restore_cancelled),
            text = notificationContentTextOnFinish(),
        )
    }

    override fun getFinishedWithErrorNotification(): NotificationInfo {
        return NotificationInfo(
            notificationId = NOTIFICATION_ID_RESTORE_FINISHED_WITH_ERRORS,
            notificationChannelId = CHANNEL_RESTORE_END_ID,
            icon = R.drawable.notification_icon_00,
            title = context.getString(R.string.restore_finished_with_errors),
            text = notificationContentTextOnFinish(),
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
        val notificationBuilder = notification.convertToNotificationBuilder(context).apply {
            setContentIntent(pendingIntent)

            if (notification.notificationId != NOTIFICATION_ID_RESTORE_ONGOING) return@apply

            setOngoing(true)
            addAction(
                notification.getCancelAction(context) {
                    Intent(context, RestoreService::class.java).apply {
                        action = ACTION_CANCEL_RESTORE
                    }
                }
            )
        }

        notificationManager.notify(
            notification.notificationId,
            notificationBuilder.build()
        )
    }
}