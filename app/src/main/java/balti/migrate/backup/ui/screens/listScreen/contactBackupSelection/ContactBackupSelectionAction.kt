package balti.migrate.backup.ui.screens.listScreen.contactBackupSelection

import baltiapps.migrate.domain.common.model.ContactListItem

sealed class ContactBackupSelectionAction {
    class ToggleContactItem(val item: ContactListItem): ContactBackupSelectionAction()
    class ToggleAllContacts(val isChecked: Boolean): ContactBackupSelectionAction()
    class StageContacts(val onStagingDone: () -> Unit): ContactBackupSelectionAction()
}