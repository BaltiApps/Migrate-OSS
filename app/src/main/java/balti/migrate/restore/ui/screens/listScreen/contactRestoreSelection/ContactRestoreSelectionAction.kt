package balti.migrate.restore.ui.screens.listScreen.contactRestoreSelection

import baltiapps.migrate.domain.common.model.ContactListItem

sealed class ContactRestoreSelectionAction {
    class ToggleContactItem(val item: ContactListItem): ContactRestoreSelectionAction()
    class ToggleAllContacts(val isChecked: Boolean): ContactRestoreSelectionAction()
    class StageContacts(val onStagingDone: () -> Unit): ContactRestoreSelectionAction()
}