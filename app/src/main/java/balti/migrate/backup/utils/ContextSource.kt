package balti.migrate.backup.utils

import android.content.Context
import balti.migrate.R
import baltiapps.migrate.domain.backup.model.Progress
import baltiapps.migrate.domain.backup.sources.PlatformContextSource

class ContextSource(private val context: Context): PlatformContextSource {
    override fun getProgressTitle(progressType: Progress.ProgressType): String {
        return when (progressType) {
            Progress.ProgressType.CONTACTS_BACKUP -> context.getString(R.string.label_contacts_backup)
            Progress.ProgressType.CALL_LOG_BACKUP -> context.getString(R.string.label_call_log_backup)
            Progress.ProgressType.SMS_BACKUP -> context.getString(R.string.label_sms_backup)
            Progress.ProgressType.BACKUP_FINISHED -> context.getString(R.string.backup_finished)
            Progress.ProgressType.BACKUP_FINISHED_WITH_ERRORS -> context.getString(R.string.backup_finished_with_errors)
            Progress.ProgressType.BACKUP_CANCELLED -> context.getString(R.string.backup_cancelled)
            else -> context.getString(R.string.loading)
        }
    }
}