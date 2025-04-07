package balti.migrate.common.data.model

import android.content.Context
import androidx.core.app.NotificationCompat

data class NotificationInfo(
    val notificationId: Int,
    val notificationChannelId: String,
    val icon: Int,
    val title: String,
    val text: String? = null,
    val progress: Int = -1,
    val maxProgress: Int = -1,
    val isIndeterminate: Boolean = false,
    val shouldShowProgress: Boolean = false,
) {
    fun convertToNotificationBuilder(
        context: Context,
    ): NotificationCompat.Builder {
        return NotificationCompat.Builder(context, notificationChannelId).apply {
            setSmallIcon(icon)
            setContentTitle(title)
            setContentText(text)
            if (shouldShowProgress) {
                setProgress(maxProgress, progress, isIndeterminate)
            }
        }
    }
}
