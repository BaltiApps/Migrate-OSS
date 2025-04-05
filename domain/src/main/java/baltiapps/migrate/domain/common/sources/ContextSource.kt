package baltiapps.migrate.domain.common.sources

import baltiapps.migrate.domain.common.model.Progress

interface ContextSource {
    fun getProgressTitle(progressType: Progress.ProgressType): String
}