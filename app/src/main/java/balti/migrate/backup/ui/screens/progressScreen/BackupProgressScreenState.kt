package balti.migrate.backup.ui.screens.progressScreen

import baltiapps.migrate.domain.common.model.Progress

data class BackupProgressScreenState(
    val progressList: List<Progress>,
    val errorList: List<Progress>,
    val headingText: String,
    val errorOnly: Boolean,
    val isCancelling: Boolean,
    val isBackupFinished: Boolean,
) {
    val isLoading: Boolean = progressList.isEmpty() && errorList.isEmpty()
    companion object {
        val Empty = BackupProgressScreenState(
            progressList = emptyList(),
            errorList = emptyList(),
            headingText = "",
            errorOnly = false,
            isCancelling = false,
            isBackupFinished = false,
        )
    }
}
