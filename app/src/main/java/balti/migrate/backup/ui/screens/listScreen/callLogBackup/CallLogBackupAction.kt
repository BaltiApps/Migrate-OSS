package balti.migrate.backup.ui.screens.listScreen.callLogBackup

import baltiapps.migrate.domain.backup.model.CallLogListItem

sealed class CallLogBackupAction {
    class ToggleCallLogItem(val item: CallLogListItem): CallLogBackupAction()
    class ToggleAllCallLog(val isChecked: Boolean): CallLogBackupAction()
    class StageCallLogs(val onStagingDone: () -> Unit): CallLogBackupAction()
}