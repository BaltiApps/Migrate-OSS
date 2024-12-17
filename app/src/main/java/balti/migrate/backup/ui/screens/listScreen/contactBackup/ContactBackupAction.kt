package balti.migrate.backup.ui.screens.listScreen.contactBackup

import baltiapps.migrate.domain.backup.model.ContactListItem

sealed class ContactBackupAction {
    class ToggleContactItem(val item: ContactListItem): ContactBackupAction()
    class ToggleAllContacts(val isChecked: Boolean): ContactBackupAction()
    class StageContacts(val onStagingDone: () -> Unit): ContactBackupAction()
}