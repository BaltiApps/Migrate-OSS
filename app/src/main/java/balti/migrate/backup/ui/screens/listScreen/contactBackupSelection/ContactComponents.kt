package balti.migrate.backup.ui.screens.listScreen.contactBackupSelection

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import balti.migrate.R
import balti.migrate.common.ui.components.RenderContactItem
import baltiapps.migrate.domain.common.model.ContactListItem

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

fun LazyListScope.showContactList(
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