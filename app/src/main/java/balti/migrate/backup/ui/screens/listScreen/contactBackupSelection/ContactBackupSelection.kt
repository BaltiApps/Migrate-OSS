package balti.migrate.backup.ui.screens.listScreen.contactBackupSelection

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import balti.migrate.R
import balti.migrate.common.ui.listScreen.ListScreenShell
import balti.migrate.common.ui.listScreen.ListState
import balti.migrate.common.utils.PermissionUtils
import baltiapps.migrate.domain.common.model.ContactListItem
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ContactBackupSelection(
    navigateUp: () -> Unit,
    goToNextScreen: () -> Unit,
    viewModel: ContactBackupSelectionViewModel = koinViewModel(),
) {
    val lifecycleOwner = LocalLifecycleOwner.current

    val state by viewModel.state.collectAsStateWithLifecycle(
        lifecycleOwner = lifecycleOwner,
    )

    SyncedWarningDialog()

    Content(
        state = { state },
        navigateUp = navigateUp,
        onSelectAll = {
            viewModel.performAction(ContactBackupSelectionAction.ToggleAllContacts(true))
        },
        onDeselectAll = {
            viewModel.performAction(ContactBackupSelectionAction.ToggleAllContacts(false))
        },
        onToggleSyncedContactsVisibility = {
            viewModel.performAction(ContactBackupSelectionAction.ToggleSyncedContactsVisibility(it))
        },
        onToggleLocalContactsVisibility = {
            viewModel.performAction(ContactBackupSelectionAction.ToggleLocalContactsVisibility(it))
        },
        requestPermission = PermissionUtils.requestPermission(PermissionUtils.contactsReadPermission) {
            viewModel.performAction(ContactBackupSelectionAction.OnPermissionResult(it))
        },
        onItemToggled = {
            viewModel.performAction(ContactBackupSelectionAction.ToggleContactItem(it))
        },
        showSyncedContactsWhyNotRecommended = {
            showSyncedContactsWarningDialog()
        },
        onNext = {
            viewModel.performAction(ContactBackupSelectionAction.StageContacts(goToNextScreen))
        }
    )
}

@Composable
private fun Content(
    state: () -> ContactBackupSelectionState,
    navigateUp: () -> Unit,
    onSelectAll: () -> Unit,
    onDeselectAll: () -> Unit,
    onToggleSyncedContactsVisibility: (isVisible: Boolean) -> Unit,
    onToggleLocalContactsVisibility: (isVisible: Boolean) -> Unit,
    requestPermission: () -> Unit,
    onItemToggled: (item: ContactListItem) -> Unit,
    showSyncedContactsWhyNotRecommended: () -> Unit,
    onNext: () -> Unit,
) {
    val syncedContactsExpanded = state().syncedContactsExpanded
    val localContactsExpanded = state().localContactsExpanded
    val syncedContactList = state().syncedContacts
    val localContactList = state().localContacts
    val shouldShowOnlySyncedContacts = localContactList.isEmpty() && syncedContactList.isNotEmpty()
    val shouldShowOnlyLocalContacts = syncedContactList.isEmpty() && localContactList.isNotEmpty()
    val shouldShowNoData = syncedContactList.isEmpty() && localContactList.isEmpty()
    val nextButtonCustomLabel = stringResource(R.string.skip).takeIf {
        shouldShowOnlySyncedContacts && !syncedContactsExpanded
    }
    val enableMultiSelectButtons =
        (localContactsExpanded && localContactList.isNotEmpty()) ||
                (syncedContactsExpanded && syncedContactList.isNotEmpty())
    val listState = ListState(
        listTitle = stringResource(R.string.label_contacts_backup),
        isStaging = state().isStaging,
        hasPermission = state().hasPermission,
        permissionDescription = stringResource(R.string.contacts_backup_permission_description),
        progress = state().progress,
        hasNoData = shouldShowNoData,
    )
    ListScreenShell(
        listState = listState,
        navigateUp = navigateUp,
        onSelectAll = onSelectAll.takeIf { enableMultiSelectButtons },
        onDeselectAll = onDeselectAll.takeIf { enableMultiSelectButtons },
        onPermissionRequest = requestPermission,
        onNext = onNext,
        nextButtonCustomLabel = nextButtonCustomLabel,
    ) {
        when {
            shouldShowOnlySyncedContacts -> OnlySyncedContacts(
                syncedContactList = syncedContactList,
                isStaging = state().isStaging,
                shouldShowContacts = syncedContactsExpanded,
                onShowContactsConfirmation = {
                    onToggleSyncedContactsVisibility(true)
                },
                onItemToggled = onItemToggled,
                showSyncedContactsWhyNotRecommended = showSyncedContactsWhyNotRecommended,
            )
            shouldShowOnlyLocalContacts -> OnlyLocalContacts(
                localContactList = localContactList,
                isStaging = state().isStaging,
                onItemToggled = onItemToggled,
            )
            else -> LocalAndSyncedContacts(
                syncedContactList = syncedContactList,
                localContactList = localContactList,
                isStaging = state().isStaging,
                syncedContactsExpanded = syncedContactsExpanded,
                localContactsExpanded = localContactsExpanded,
                onToggleSyncedContactsVisibility = onToggleSyncedContactsVisibility,
                onToggleLocalContactsVisibility = onToggleLocalContactsVisibility,
                onItemToggled = onItemToggled,
            )
        }
    }
}
