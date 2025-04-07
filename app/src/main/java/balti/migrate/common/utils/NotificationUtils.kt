package balti.migrate.common.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import androidx.core.app.NotificationCompat
import balti.migrate.common.data.model.NotificationInfo

class NotificationUtils {
    fun NotificationManager.makeNotificationChannel(
        channelId: String,
        channelDesc: String,
        importance: Int,
    ) {
        createNotificationChannel(
            NotificationChannel(channelId, channelDesc, importance)
        )
    }

    fun NotificationCompat.Builder.modifyNotificationWithProgress(
        notificationInfo: NotificationInfo
    ) {
        notificationInfo.run {
            setOngoing(
                progress < maxProgress
            )
            if (title.isNotBlank()) {
                setContentTitle(title)
            }
            if (!text.isNullOrBlank()) {
                setContentText(text)
            }
            if (maxProgress > 0) {
                setProgress(maxProgress, progress, false)
            } else {
                setProgress(0, 0, true)
            }
        }
    }
}