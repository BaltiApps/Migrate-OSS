package balti.migrate.backup.ui.screens.listScreen.callLogBackupSelection

import android.app.Activity
import baltiapps.migrate.domain.common.model.CallLogListItem

sealed class CallLogBackupSelectionAction {
    data class RequestPermission(val activity: Activity?): CallLogBackupSelectionAction()
    data class ToggleCallLogItem(val item: CallLogListItem): CallLogBackupSelectionAction()
    data class ToggleAllCallLog(val isChecked: Boolean): CallLogBackupSelectionAction()
    data class StageCallLogs(val onStagingDone: () -> Unit): CallLogBackupSelectionAction()
}