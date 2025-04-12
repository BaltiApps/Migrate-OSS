package balti.migrate.backup.ui.screens.listScreen.callLogBackupSelection

import baltiapps.migrate.domain.common.model.CallLogListItem

sealed class CallLogBackupSelectionAction {
    class ToggleCallLogItem(val item: CallLogListItem): CallLogBackupSelectionAction()
    class ToggleAllCallLog(val isChecked: Boolean): CallLogBackupSelectionAction()
    class StageCallLogs(val onStagingDone: () -> Unit): CallLogBackupSelectionAction()
}