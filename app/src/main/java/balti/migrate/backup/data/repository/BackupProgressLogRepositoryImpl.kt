package balti.migrate.backup.data.repository

import android.content.Context
import balti.migrate.R
import baltiapps.migrate.domain.backup.model.Progress
import baltiapps.migrate.domain.backup.repository.BackupProgressLogRepository

class BackupProgressLogRepositoryImpl (
    applicationContext: Context,
): BackupProgressLogRepository() {
    override val truncatedIndicator: Progress = Progress.Empty.copy(
        logs = "${applicationContext.getString(R.string.older_logs_truncated)}\n"
    )
}