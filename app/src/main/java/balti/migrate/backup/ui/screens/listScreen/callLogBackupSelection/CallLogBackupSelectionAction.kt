package balti.migrate.backup.ui.screens.listScreen.callLogBackupSelection

import baltiapps.migrate.domain.common.model.CallLogListItem

sealed class CallLogBackupSelectionAction {
    data class OnPermissionResult(val isGranted: Boolean): CallLogBackupSelectionAction()
    data class ToggleCallLogItem(val item: CallLogListItem): CallLogBackupSelectionAction()
    data class ToggleAllCallLog(val isChecked: Boolean): CallLogBackupSelectionAction()
    data class StageCallLogs(val onStagingDone: () -> Unit): CallLogBackupSelectionAction()
}