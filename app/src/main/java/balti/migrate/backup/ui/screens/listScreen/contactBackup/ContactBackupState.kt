package balti.migrate.backup.ui.screens.listScreen.contactBackup

import baltiapps.migrate.domain.backup.model.ContactListItem
import baltiapps.migrate.domain.common.model.Progress

data class ContactBackupState(
    val progress: Progress = Progress.Empty,
    val contactList: List<ContactListItem> = emptyList(),
    val isStaging: Boolean = false,
)
