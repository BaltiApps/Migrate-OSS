package balti.migrate.common.data.repository

import android.content.Context
import balti.migrate.R
import baltiapps.migrate.domain.common.model.Progress
import baltiapps.migrate.domain.common.repository.ProgressLogRepository

class ProgressLogRepositoryImpl (
    applicationContext: Context,
): ProgressLogRepository() {
    override val truncatedIndicator: Progress = Progress.Empty.copy(
        logs = "${applicationContext.getString(R.string.older_logs_truncated)}\n"
    )
}