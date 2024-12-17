package balti.migrate.backup.ui.screens.listScreen.smsBackup

import baltiapps.migrate.domain.backup.model.SmsListItem

sealed class SmsBackupAction {
    class ToggleSmsItem(val item: SmsListItem): SmsBackupAction()
    class ToggleAllSms(val isChecked: Boolean): SmsBackupAction()
    class StageSms(val onStagingDone: () -> Unit): SmsBackupAction()
}