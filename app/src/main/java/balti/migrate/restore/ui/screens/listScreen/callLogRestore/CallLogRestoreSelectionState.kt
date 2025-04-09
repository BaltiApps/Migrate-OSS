package balti.migrate.restore.ui.screens.listScreen.callLogRestore

import baltiapps.migrate.domain.common.model.CallLogListItem
import baltiapps.migrate.domain.common.model.Progress

data class CallLogRestoreSelectionState(
    val progress: Progress = Progress.Empty,
    val callLogList: List<CallLogListItem> = emptyList(),
    val isStaging: Boolean = false,
)