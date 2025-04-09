package balti.migrate.common.data.sources

import android.content.Context
import balti.migrate.R
import baltiapps.migrate.domain.common.model.Progress
import baltiapps.migrate.domain.common.sources.ContextSource

class ContextSourceImpl(private val context: Context): ContextSource {
    override fun getProgressTitle(progressType: Progress.ProgressType): String {
        return when (progressType) {
            Progress.ProgressType.CONTACTS_BACKUP -> context.getString(R.string.label_contacts_backup)
            Progress.ProgressType.CALL_LOG_BACKUP -> context.getString(R.string.label_call_log_backup)
            Progress.ProgressType.SMS_BACKUP -> context.getString(R.string.label_sms_backup)
            Progress.ProgressType.BACKUP_FINISHED -> context.getString(R.string.backup_finished)
            Progress.ProgressType.BACKUP_FINISHED_WITH_ERRORS -> context.getString(R.string.backup_finished_with_errors)
            Progress.ProgressType.BACKUP_CANCELLED -> context.getString(R.string.backup_cancelled)
            Progress.ProgressType.CALL_LOG_RESTORE -> context.getString(R.string.label_call_log_restore)
            else -> context.getString(R.string.loading)
        }
    }
}