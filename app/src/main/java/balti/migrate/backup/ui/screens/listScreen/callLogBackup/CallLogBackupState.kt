package balti.migrate.backup.ui.screens.listScreen.callLogBackup

import baltiapps.migrate.domain.common.model.CallLogListItem
import baltiapps.migrate.domain.common.model.Progress

data class CallLogBackupState(
    val progress: Progress = Progress.Empty,
    val callLogList: List<CallLogListItem> = emptyList(),
    val isStaging: Boolean = false,
)