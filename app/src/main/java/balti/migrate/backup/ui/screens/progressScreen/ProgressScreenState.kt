package balti.migrate.backup.ui.screens.progressScreen

import baltiapps.migrate.domain.common.model.Progress

data class ProgressScreenState(
    val progressList: List<Progress>,
    val headingText: String,
    val errorOnly: Boolean,
    val isCancelling: Boolean,
    val isBackupFinished: Boolean,
) {
    companion object {
        val Empty = ProgressScreenState(
            progressList = emptyList(),
            headingText = "",
            errorOnly = false,
            isCancelling = false,
            isBackupFinished = false,
        )
    }
}
