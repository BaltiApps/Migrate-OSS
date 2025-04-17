package balti.migrate.restore.ui.screens.listScreen.contactRestoreSelection

import baltiapps.migrate.domain.common.model.ContactListItem

sealed class ContactRestoreSelectionAction {
    data class ToggleContactItem(val item: ContactListItem): ContactRestoreSelectionAction()
    data class ToggleAllContacts(val isChecked: Boolean): ContactRestoreSelectionAction()
    data class StageContacts(val onStagingDone: () -> Unit): ContactRestoreSelectionAction()
}