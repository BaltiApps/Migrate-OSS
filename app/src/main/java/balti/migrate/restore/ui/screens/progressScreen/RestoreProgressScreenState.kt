package balti.migrate.restore.ui.screens.progressScreen

import baltiapps.migrate.domain.common.model.Progress

data class RestoreProgressScreenState(
    val progressList: List<Progress>,
    val headingText: String,
    val errorOnly: Boolean,
    val isCancelling: Boolean,
    val isRestoreFinished: Boolean,
) {
    companion object {
        val Empty = RestoreProgressScreenState(
            progressList = emptyList(),
            headingText = "",
            errorOnly = false,
            isCancelling = false,
            isRestoreFinished = false,
        )
    }
}
