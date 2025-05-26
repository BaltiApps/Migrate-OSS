package balti.migrate.backup.ui.screens.listScreen.contactBackupSelection

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
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
    val syncedContactCount = syncedContactList.size
    val localContactCount = localContactList.size
    val selectedSyncedContactCount = syncedContactList.filter { it.isChecked }.size
    val selectedLocalContactCount = localContactList.filter { it.isChecked }.size
    val syncedCountString = "$selectedSyncedContactCount / $syncedContactCount"
    val localCountString = "$selectedLocalContactCount / $localContactCount"
    LazyColumn(
        modifier = modifier.fillMaxSize(),
    ) {
        stickyHeader {
            ContactHeader(
                header = {
                    Text(
                        text = stringResource(R.string.synced_contacts),
                        style = MaterialTheme.typography.labelLarge,
                    )
                    Text(
                        text = "${stringResource(R.string.selected_items)} - $syncedCountString",
                        style = MaterialTheme.typography.labelLarge,
                    )
                },
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
                shouldShowCountBar = false,
            )
        }
        item {
            Spacer(modifier = Modifier.padding(4.dp))
        }
        stickyHeader {
            ContactHeader(
                header = {
                    Text(
                        text = stringResource(R.string.local_contacts),
                        style = MaterialTheme.typography.labelLarge,
                    )
                    Text(
                        text = "${stringResource(R.string.selected_items)} - $localCountString",
                        style = MaterialTheme.typography.labelLarge,
                    )
                },
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
                shouldShowCountBar = false,
            )
        }
    }
}

@Preview
@Composable
private fun ContactPreview() {
    val syncedContacts = listOf<ContactListItem>(
        ContactListItem(
            _id = "1",
            displayName = "s name1",
            isLocalContact = false,
            isChecked = false,
        ),
        ContactListItem(
            _id = "2",
            displayName = "s name2",
            isLocalContact = false,
            isChecked = true,
        ),
        ContactListItem(
            _id = "3",
            displayName = "s name3",
            isLocalContact = false,
            isChecked = false,
        ),
        ContactListItem(
            _id = "4",
            displayName = "s name4",
            isLocalContact = false,
            isChecked = true,
        ),
        ContactListItem(
            _id = "5",
            displayName = "s name5",
            isLocalContact = false,
            isChecked = false,
        ),
        ContactListItem(
            _id = "6",
            displayName = "s name6",
            isLocalContact = false,
            isChecked = true,
        ),
        ContactListItem(
            _id = "7",
            displayName = "s name7",
            isLocalContact = false,
            isChecked = false,
        ),
        ContactListItem(
            _id = "8",
            displayName = "s name8",
            isLocalContact = false,
            isChecked = true,
        )
    )
    val localContacts = listOf<ContactListItem>(
        ContactListItem(
            _id = "9",
            displayName = "l name1",
            isLocalContact = true,
            isChecked = false,
        ),
        ContactListItem(
            _id = "10",
            displayName = "l name2",
            isLocalContact = true,
            isChecked = true,
        ),
        ContactListItem(
            _id = "11",
            displayName = "l name3",
            isLocalContact = true,
            isChecked = false,
        ),
        ContactListItem(
            _id = "12",
            displayName = "l name4",
            isLocalContact = true,
            isChecked = true,
        ),
        ContactListItem(
            _id = "13",
            displayName = "l name5",
            isLocalContact = true,
            isChecked = false,
        ),
        ContactListItem(
            _id = "14",
            displayName = "l name6",
            isLocalContact = true,
            isChecked = true,
        ),
        ContactListItem(
            _id = "15",
            displayName = "l name7",
            isLocalContact = true,
            isChecked = false,
        )
    )
    var syncedContactsExpanded by remember { mutableStateOf(false) }
    var localContactsExpanded by remember { mutableStateOf(true) }
    LocalAndSyncedContacts(
        syncedContactList = syncedContacts,
        localContactList = localContacts,
        isStaging = false,
        syncedContactsExpanded = syncedContactsExpanded,
        localContactsExpanded = localContactsExpanded,
        onToggleSyncedContactsVisibility = { syncedContactsExpanded = it },
        onToggleLocalContactsVisibility = { localContactsExpanded = it },
        onItemToggled = {},
    )
}