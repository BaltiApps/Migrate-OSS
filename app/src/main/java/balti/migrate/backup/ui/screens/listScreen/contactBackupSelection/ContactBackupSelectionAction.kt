package balti.migrate.backup.ui.screens.listScreen.contactBackupSelection

import android.app.Activity
import baltiapps.migrate.domain.common.model.ContactListItem

sealed class ContactBackupSelectionAction {
    data class RequestPermission(val activity: Activity?): ContactBackupSelectionAction()
    class ToggleContactItem(val item: ContactListItem): ContactBackupSelectionAction()
    class ToggleAllContacts(val isChecked: Boolean): ContactBackupSelectionAction()
    class StageContacts(val onStagingDone: () -> Unit): ContactBackupSelectionAction()
    class ToggleSyncedContactsVisibility(val isVisible: Boolean): ContactBackupSelectionAction()
    class ToggleLocalContactsVisibility(val isVisible: Boolean): ContactBackupSelectionAction()
}