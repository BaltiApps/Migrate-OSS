package balti.migrate.restore.ui.screens.listScreen.callLogRestoreSelection

import baltiapps.migrate.domain.common.model.CallLogListItem

sealed class CallLogRestoreSelectionAction {
    class ToggleCallLogItem(val item: CallLogListItem): CallLogRestoreSelectionAction()
    class ToggleAllCallLog(val isChecked: Boolean): CallLogRestoreSelectionAction()
    class StageCallLogs(val onStagingDone: () -> Unit): CallLogRestoreSelectionAction()
}