package balti.migrate.restore.data.sources

import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
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
        private const val CHANNEL_RESTORE_CANCELLING_ID = "restore_cancelling"
        private const val CHANNEL_RESTORE_CANCELLING_DESC = "Cancelling current restore"

        private const val NOTIFICATION_ID_RESTORE_ONGOING = 230
        private const val NOTIFICATION_ID_RESTORE_COMPLETE = 231
        private const val NOTIFICATION_ID_RESTORE_CANCELLED = 232
    }

    private val notificationManager by lazy {
        context.getSystemService(NotificationManager::class.java)
    }

    private var currentNotificationInfo: NotificationInfo? = null
    private var currentNotificationBuilder: NotificationCompat.Builder? = null

    private val notificationUtils = NotificationUtils()

    override fun setup() {
        Timber.i("Creating progress sample job")
        progressSamplerJob = Job()

        notificationUtils.run {
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
            notificationManager.makeNotificationChannel(
                channelId = CHANNEL_RESTORE_CANCELLING_ID,
                channelDesc = CHANNEL_RESTORE_CANCELLING_DESC,
                importance = NotificationManager.IMPORTANCE_MIN
            )
        }

        getInitialNotification()
    }

    override fun getInitialNotification(): NotificationInfo {
        val initialNotification = NotificationInfo(
            notificationId = NOTIFICATION_ID_RESTORE_ONGOING,
            notificationChannelId = CHANNEL_RESTORE_RUNNING_ID,
            icon = R.drawable.notification_rotating_icon,
            title = context.getString(R.string.loading),
            shouldShowProgress = true,
            isIndeterminate = true,
        )
        currentNotificationInfo = initialNotification
        currentNotificationBuilder = initialNotification.convertToNotificationBuilder(context)
        return initialNotification
    }

    override suspend fun getLatestProgress(): Progress {
        return progressLogRepository.getLatestProgress()
    }

    override fun getProgressNotification(progress: Progress): NotificationInfo {
        val progressInt = (progress.percentage*100).roundToInt()
        val title = contextSource.getProgressTitle(progress.progressType)

        return (currentNotificationInfo ?: getInitialNotification()).copy(
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
            currentNotificationBuilder?.modifyNotificationWithProgress(notification)
            currentNotificationBuilder?.run { notificationManager.notify(notification.notificationId, this.build()) }
        }
    }
}