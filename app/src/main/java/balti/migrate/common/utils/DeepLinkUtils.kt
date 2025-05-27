package balti.migrate.common.utils

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.net.toUri
import balti.migrate.BuildConfig
import balti.migrate.app.MainActivity

class DeepLinkUtils {

    enum class MigrateUri(val uriString: String) {
        UriProgressBackup("${BuildConfig.SCHEMA}://${BuildConfig.HOST_PROGRESS_BACKUP}"),
        UriProgressRestore("${BuildConfig.SCHEMA}://${BuildConfig.HOST_PROGRESS_RESTORE}"),
        ;

        val uri: Uri get() = uriString.toUri()
    }

    fun getPendingIntent(uri: MigrateUri, context: Context): PendingIntent {
        val intent = Intent(
            Intent.ACTION_VIEW,
            uri.uri,
            context,
            MainActivity::class.java
        )
        return PendingIntent.getActivity(
            context,
            uri.ordinal + 1,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )
    }
}