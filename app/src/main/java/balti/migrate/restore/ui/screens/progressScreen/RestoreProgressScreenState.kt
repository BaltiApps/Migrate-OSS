package balti.migrate.restore.ui.screens.progressScreen

import baltiapps.migrate.domain.common.model.Progress

data class RestoreProgressScreenState(
    val progressList: List<Progress>,
    val errorList: List<Progress>,
    val headingText: String,
    val errorOnly: Boolean,
    val isCancelling: Boolean,
    val isRestoreFinished: Boolean,
    val shouldChangeSmsApp: Boolean,
) {
    val isLoading: Boolean get() = progressList.isEmpty() && errorList.isEmpty()
    companion object {
        val Empty = RestoreProgressScreenState(
            progressList = emptyList(),
            errorList = emptyList(),
            headingText = "",
            errorOnly = false,
            isCancelling = false,
            isRestoreFinished = false,
            shouldChangeSmsApp = false,
        )
    }
}
