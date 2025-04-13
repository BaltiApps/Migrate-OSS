package balti.migrate.restore.ui.screens.listScreen.callLogRestoreSelection

import android.app.Activity
import baltiapps.migrate.domain.common.model.CallLogListItem

sealed class CallLogRestoreSelectionAction {
    data class RequestPermission(val activity: Activity?): CallLogRestoreSelectionAction()
    class ToggleCallLogItem(val item: CallLogListItem): CallLogRestoreSelectionAction()
    class ToggleAllCallLog(val isChecked: Boolean): CallLogRestoreSelectionAction()
    class StageCallLogs(val onStagingDone: () -> Unit): CallLogRestoreSelectionAction()
}