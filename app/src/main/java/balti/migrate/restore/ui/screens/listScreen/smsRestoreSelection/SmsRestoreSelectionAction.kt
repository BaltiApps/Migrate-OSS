package balti.migrate.restore.ui.screens.listScreen.smsRestoreSelection

import baltiapps.migrate.domain.common.model.SmsListItem

sealed class SmsRestoreSelectionAction {
    data class ToggleSmsItem(val item: SmsListItem) : SmsRestoreSelectionAction()
    data class ToggleAllSms(val isChecked: Boolean) : SmsRestoreSelectionAction()
    data class StageSms(val onStagingDone: () -> Unit) : SmsRestoreSelectionAction()
}