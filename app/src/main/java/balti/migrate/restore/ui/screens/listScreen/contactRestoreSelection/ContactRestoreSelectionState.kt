package balti.migrate.restore.ui.screens.listScreen.contactRestoreSelection

import baltiapps.migrate.domain.common.model.ContactListItem
import baltiapps.migrate.domain.common.model.Progress

data class ContactRestoreSelectionState(
    val progress: Progress = Progress.Empty,
    val contactListItems: List<ContactListItem> = emptyList(),
    val isStaging: Boolean = false,
)