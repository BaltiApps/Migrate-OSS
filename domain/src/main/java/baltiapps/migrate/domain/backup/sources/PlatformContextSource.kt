package baltiapps.migrate.domain.backup.sources

import baltiapps.migrate.domain.backup.model.Progress

interface PlatformContextSource {
    fun getProgressTitle(progressType: Progress.ProgressType): String
}