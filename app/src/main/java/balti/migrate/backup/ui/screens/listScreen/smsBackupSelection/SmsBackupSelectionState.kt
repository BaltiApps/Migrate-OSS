package balti.migrate.backup.ui.screens.listScreen.smsBackupSelection

import baltiapps.migrate.domain.common.model.Progress
import baltiapps.migrate.domain.common.model.SmsListItem

data class SmsBackupSelectionState(
    val progress: Progress = Progress.Empty,
    val smsList: List<SmsListItem> = emptyList(),
    val isStaging: Boolean = false,
)
