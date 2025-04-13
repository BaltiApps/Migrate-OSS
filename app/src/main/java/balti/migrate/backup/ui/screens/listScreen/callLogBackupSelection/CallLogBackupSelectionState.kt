package balti.migrate.backup.ui.screens.listScreen.callLogBackupSelection

import baltiapps.migrate.domain.common.model.CallLogListItem
import baltiapps.migrate.domain.common.model.Progress

data class CallLogBackupSelectionState(
    val progress: Progress = Progress.Empty,
    val callLogList: List<CallLogListItem> = emptyList(),
    val isStaging: Boolean = false,
    val hasPermission: Boolean = true,
)