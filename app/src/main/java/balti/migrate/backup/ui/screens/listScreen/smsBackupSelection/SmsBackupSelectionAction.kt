package balti.migrate.backup.ui.screens.listScreen.smsBackupSelection

import baltiapps.migrate.domain.common.model.SmsListItem

sealed class SmsBackupSelectionAction {
    class ToggleSmsItem(val item: SmsListItem): SmsBackupSelectionAction()
    class ToggleAllSms(val isChecked: Boolean): SmsBackupSelectionAction()
    class StageSms(val onStagingDone: () -> Unit): SmsBackupSelectionAction()
}