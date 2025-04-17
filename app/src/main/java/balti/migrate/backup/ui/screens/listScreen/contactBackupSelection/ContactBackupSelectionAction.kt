package balti.migrate.backup.ui.screens.listScreen.contactBackupSelection

import android.app.Activity
import baltiapps.migrate.domain.common.model.ContactListItem

sealed class ContactBackupSelectionAction {
    data class RequestPermission(val activity: Activity?): ContactBackupSelectionAction()
    data class ToggleContactItem(val item: ContactListItem): ContactBackupSelectionAction()
    data class ToggleAllContacts(val isChecked: Boolean): ContactBackupSelectionAction()
    data class StageContacts(val onStagingDone: () -> Unit): ContactBackupSelectionAction()
    data class ToggleSyncedContactsVisibility(val isVisible: Boolean): ContactBackupSelectionAction()
    data class ToggleLocalContactsVisibility(val isVisible: Boolean): ContactBackupSelectionAction()
}