package baltiapps.migrate.domain.backup.sources

import baltiapps.migrate.domain.backup.model.Progress

interface ContextSource {
    fun getProgressTitle(progressType: Progress.ProgressType): String
}