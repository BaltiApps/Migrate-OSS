package balti.migrate.backup.ui.screens.listScreen.contactBackupSelection

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import balti.migrate.R
import baltiapps.migrate.domain.common.model.ContactListItem


@Composable
fun LocalAndSyncedContacts(
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