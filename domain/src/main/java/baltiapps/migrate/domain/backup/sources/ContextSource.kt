package baltiapps.migrate.domain.backup.sources

import baltiapps.migrate.domain.common.model.Progress

interface ContextSource {
    fun getProgressTitle(progressType: Progress.ProgressType): String
}