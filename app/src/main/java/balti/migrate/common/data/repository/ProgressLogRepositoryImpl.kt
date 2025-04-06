package balti.migrate.common.data.repository

import android.content.Context
import balti.migrate.R
import baltiapps.migrate.domain.common.model.Progress
import baltiapps.migrate.domain.common.repository.ProgressLogRepository

open class ProgressLogRepositoryImpl (
    applicationContext: Context,
): ProgressLogRepository() {

    companion object {
        const val BREAK_LINE = "===================================="
    }

    override val truncatedIndicator: Progress = Progress.Empty.copy(
        logs = "${applicationContext.getString(R.string.older_logs_truncated)}\n${BREAK_LINE}\n"
    )
}