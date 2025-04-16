package balti.migrate.backup.ui.screens.listScreen.contactBackupSelection

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import baltiapps.migrate.domain.common.model.ContactListItem

@Composable
fun OnlyLocalContacts(
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