package balti.migrate.restore.ui.screens.listScreen.smsRestoreSelection

import baltiapps.migrate.domain.common.model.Progress
import baltiapps.migrate.domain.common.model.SmsListItem

data class SmsRestoreSelectionState(
    val progress: Progress = Progress.Empty,
    val smsList: List<SmsListItem> = emptyList(),
    val isStaging: Boolean = false,
)
