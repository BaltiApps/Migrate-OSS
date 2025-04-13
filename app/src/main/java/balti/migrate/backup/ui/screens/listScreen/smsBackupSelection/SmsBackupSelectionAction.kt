package balti.migrate.backup.ui.screens.listScreen.smsBackupSelection

import android.app.Activity
import baltiapps.migrate.domain.common.model.SmsListItem

sealed class SmsBackupSelectionAction {
    data class RequestPermission(val activity: Activity?): SmsBackupSelectionAction()
    class ToggleSmsItem(val item: SmsListItem): SmsBackupSelectionAction()
    class ToggleAllSms(val isChecked: Boolean): SmsBackupSelectionAction()
    class StageSms(val onStagingDone: () -> Unit): SmsBackupSelectionAction()
}