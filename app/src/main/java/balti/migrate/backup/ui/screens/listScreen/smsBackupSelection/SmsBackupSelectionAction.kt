package balti.migrate.backup.ui.screens.listScreen.smsBackupSelection

import android.app.Activity
import baltiapps.migrate.domain.common.model.SmsListItem

sealed class SmsBackupSelectionAction {
    data class RequestPermission(val activity: Activity?): SmsBackupSelectionAction()
    data class ToggleSmsItem(val item: SmsListItem): SmsBackupSelectionAction()
    data class ToggleAllSms(val isChecked: Boolean): SmsBackupSelectionAction()
    data class StageSms(val onStagingDone: () -> Unit): SmsBackupSelectionAction()
}