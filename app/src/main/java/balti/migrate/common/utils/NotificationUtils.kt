package balti.migrate.common.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import balti.migrate.common.data.model.NotificationInfo
import balti.migrate.R
import android.app.PendingIntent
import android.content.Intent

fun NotificationManager.makeNotificationChannel(
    channelId: String,
    channelDesc: String,
    importance: Int,
) {
    createNotificationChannel(
        NotificationChannel(channelId, channelDesc, importance)
    )
}

fun NotificationInfo.convertToNotificationBuilder(
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

fun NotificationInfo.getCancelAction(
    context: Context,
    intent: () -> Intent
): NotificationCompat.Action {
    return NotificationCompat.Action(
        R.drawable.outline_close_24,
        context.getString(R.string.cancel),
        PendingIntent.getService(
            context,
            notificationId,
            intent(),
            PendingIntent.FLAG_IMMUTABLE,
        ),
    )
}