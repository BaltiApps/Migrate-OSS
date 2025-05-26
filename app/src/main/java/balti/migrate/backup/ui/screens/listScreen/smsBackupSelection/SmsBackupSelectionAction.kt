package balti.migrate.backup.ui.screens.listScreen.smsBackupSelection

import baltiapps.migrate.domain.common.model.SmsListItem

sealed class SmsBackupSelectionAction {
    data class OnPermissionResult(val isGranted: Boolean): SmsBackupSelectionAction()
    data class ToggleSmsItem(val item: SmsListItem): SmsBackupSelectionAction()
    data class ToggleAllSms(val isChecked: Boolean): SmsBackupSelectionAction()
    data class StageSms(val onStagingDone: () -> Unit): SmsBackupSelectionAction()
}