package balti.migrate.backup.ui.screens.listScreen.contactBackupSelection

import baltiapps.migrate.domain.common.model.ContactListItem
import baltiapps.migrate.domain.common.model.Progress

data class ContactBackupSelectionState(
    val progress: Progress = Progress.Empty,
    val contactList: List<ContactListItem> = emptyList(),
    val isStaging: Boolean = false,
    val hasPermission: Boolean = true,
    val syncedContactsExpanded: Boolean = false,
    val localContactsExpanded: Boolean = true,
) {
    val syncedContacts = contactList.filter { !it.isLocalContact }
    val localContacts = contactList.filter { it.isLocalContact }
}
