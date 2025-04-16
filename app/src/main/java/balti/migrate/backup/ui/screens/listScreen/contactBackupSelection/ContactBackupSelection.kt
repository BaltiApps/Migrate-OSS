package balti.migrate.backup.ui.screens.listScreen.contactBackupSelection

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
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

    SyncedWarningDialog()

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

@Composable
private fun OnlySyncedContacts(
    syncedContactList: List<ContactListItem>,
    isStaging: Boolean,
    shouldShowContacts: Boolean,
    onShowContactsConfirmation: () -> Unit,
    onItemToggled: (item: ContactListItem) -> Unit,
    modifier: Modifier = Modifier
) {
    if (shouldShowContacts) {
        LazyColumn(
            modifier = modifier
                .padding(8.dp)
                .fillMaxSize()
        ) {
            showContactList(
                contactList = syncedContactList,
                isStaging = isStaging,
                onItemToggled = onItemToggled,
            )
        }
    } else {
        Column(
            modifier = modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = stringResource(R.string.all_contacts_are_synced_contacts),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.size(12.dp))
            Text(
                text = stringResource(R.string.synced_contacts_backup_is_not_recommended_expanded),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.size(12.dp))
            TextButton(
                onClick = { showSyncedContactsWarningDialog() }
            ) {
                Text(stringResource(R.string.why))
            }
            OutlinedButton(
                onClick = onShowContactsConfirmation,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
            ) {
                Text(stringResource(R.string.show_synced_contacts_anyway))
            }
        }
    }
}

@Composable
private fun OnlyLocalContacts(
    localContactList: List<ContactListItem>,
    isStaging: Boolean,
    onItemToggled: (item: ContactListItem) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize()
    ) {
        showContactList(
            contactList = localContactList,
            isStaging = isStaging,
            onItemToggled = onItemToggled,
        )
    }
}

@Composable
private fun LocalAndSyncedContacts(
    syncedContactList: List<ContactListItem>,
    localContactList: List<ContactListItem>,
    isStaging: Boolean,
    syncedContactsExpanded: Boolean,
    localContactsExpanded: Boolean,
    onToggleSyncedContactsVisibility: (isVisible: Boolean) -> Unit,
    onToggleLocalContactsVisibility: (isVisible: Boolean) -> Unit,
    onItemToggled: (item: ContactListItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
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
                contactList = syncedContactList,
                isStaging = isStaging,
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
                contactList = localContactList,
                isStaging = isStaging,
                onItemToggled = onItemToggled,
            )
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