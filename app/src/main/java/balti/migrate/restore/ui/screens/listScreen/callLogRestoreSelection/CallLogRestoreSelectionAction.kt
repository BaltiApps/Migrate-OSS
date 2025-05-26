package balti.migrate.restore.ui.screens.listScreen.callLogRestoreSelection

import baltiapps.migrate.domain.common.model.CallLogListItem

sealed class CallLogRestoreSelectionAction {
    data class OnPermissionResult(val isGranted: Boolean): CallLogRestoreSelectionAction()
    data class ToggleCallLogItem(val item: CallLogListItem): CallLogRestoreSelectionAction()
    data class ToggleAllCallLog(val isChecked: Boolean): CallLogRestoreSelectionAction()
    data class StageCallLogs(val onStagingDone: () -> Unit): CallLogRestoreSelectionAction()
}