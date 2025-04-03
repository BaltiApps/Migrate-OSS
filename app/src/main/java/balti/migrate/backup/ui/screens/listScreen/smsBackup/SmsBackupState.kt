package balti.migrate.backup.ui.screens.listScreen.smsBackup

import baltiapps.migrate.domain.common.model.Progress
import baltiapps.migrate.domain.backup.model.SmsListItem

data class SmsBackupState(
    val progress: Progress = Progress.Empty,
    val smsList: List<SmsListItem> = emptyList(),
    val isStaging: Boolean = false,
)
