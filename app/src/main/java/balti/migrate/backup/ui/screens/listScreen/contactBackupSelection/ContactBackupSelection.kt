package balti.migrate.backup.ui.screens.listScreen.contactBackupSelection

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import balti.migrate.R
import balti.migrate.common.ui.components.RenderContactItem
import balti.migrate.common.ui.listScreen.ListScreenShell
import balti.migrate.common.ui.listScreen.ListState
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

    val activity = LocalActivity.current

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
        requestPermission = {
            viewModel.performAction(ContactBackupSelectionAction.RequestPermission(activity))
        },
        onItemToggled = {
            viewModel.performAction(ContactBackupSelectionAction.ToggleContactItem(it))
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
    onNext: () -> Unit,
) {
    val listState = ListState(
        listTitle = stringResource(R.string.label_contacts_backup),
        isStaging = state().isStaging,
        hasPermission = state().hasPermission,
        permissionDescription = stringResource(R.string.contacts_backup_permission_description),
        progress = state().progress,
        hasNoData = state().contactList.isEmpty(),
    )
    ListScreenShell(
        listState = listState,
        navigateUp = navigateUp,
        onSelectAll = onSelectAll,
        onDeselectAll = onDeselectAll,
        onPermissionRequest = requestPermission,
        onNext = onNext,
    ) {
        val syncedContactsExpanded = state().syncedContactsExpanded
        val localContactsExpanded = state().localContactsExpanded
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
        ) {
            item {
                ContactHeader(
                    header = stringResource(R.string.synced_contacts),
                    isExpanded = syncedContactsExpanded,
                    onClick = {
                        onToggleSyncedContactsVisibility(!syncedContactsExpanded)
                    },
                )
            }
            if (syncedContactsExpanded) {
                showContactList(
                    contactList = state().syncedContacts,
                    isStaging = listState.isStaging,
                    onItemToggled = onItemToggled,
                )
            }
            item {
                Spacer(modifier = Modifier.padding(4.dp))
            }
            item {
                ContactHeader(
                    header = stringResource(R.string.local_contacts),
                    isExpanded = localContactsExpanded,
                    onClick = {
                        onToggleLocalContactsVisibility(!localContactsExpanded)
                    }
                )
            }
            if (localContactsExpanded) {
                showContactList(
                    contactList = state().localContacts,
                    isStaging = listState.isStaging,
                    onItemToggled = onItemToggled,
                )
            }
        }
    }
}

@Composable
fun ContactHeader(
    header: String,
    isExpanded: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = header,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.labelLarge,
        )
        Image(
            imageVector = if (isExpanded) {
                Icons.Default.KeyboardArrowUp
            } else Icons.Default.KeyboardArrowDown,
            contentDescription = if (isExpanded) {
                stringResource(R.string.collapse_list)
            } else stringResource(R.string.expand_list),
            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface)
        )
    }
}

private fun LazyListScope.showContactList(
    contactList: List<ContactListItem>,
    isStaging: Boolean,
    onItemToggled: (item: ContactListItem) -> Unit,
    modifier: Modifier = Modifier
) {
    items(
        items = contactList,
        key = { it._id }
    ) { item ->
        RenderContactItem(
            item = item,
            enabled = !isStaging,
            onItemToggled = onItemToggled,
        )
    }
}