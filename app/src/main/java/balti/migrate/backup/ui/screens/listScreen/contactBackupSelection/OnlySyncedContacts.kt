package balti.migrate.backup.ui.screens.listScreen.contactBackupSelection

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import balti.migrate.R
import baltiapps.migrate.domain.common.model.ContactListItem

@Composable
fun OnlySyncedContacts(
    syncedContactList: List<ContactListItem>,
    isStaging: Boolean,
    shouldShowContacts: Boolean,
    onShowContactsConfirmation: () -> Unit,
    onItemToggled: (item: ContactListItem) -> Unit,
    showSyncedContactsWhyNotRecommended: () -> Unit,
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
                onClick = { showSyncedContactsWhyNotRecommended() }
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