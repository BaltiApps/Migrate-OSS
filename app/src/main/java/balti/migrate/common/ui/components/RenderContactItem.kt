package balti.migrate.common.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import baltiapps.migrate.domain.common.model.ContactListItem

@Composable
fun RenderContactItem(
    item: ContactListItem,
    onItemToggled: (ContactListItem) -> Unit
) {
    ListItem(
        modifier = Modifier.clickable {
            onItemToggled(item)
        },
        headlineContent = {
            Text(
                text = item.displayName.takeIf { it.isNotBlank() }
                    ?: item.displayNumber
            )
        },
        supportingContent = {
            if (item.displayName.isNotBlank()) {
                Text(item.displayNumber)
            }
        },
        trailingContent = {
            Checkbox(
                checked = item.isChecked,
                onCheckedChange = null,
            )
        }
    )
}

@Preview
@Composable
private fun ContactDisplayItemPreview() {
    val item1 = ContactListItem(
        _id = "",
        displayName = "John Price",
        displayNumber = "5563758676",
        isChecked = true,
    )
    val item2 = ContactListItem(
        _id = "",
        displayName = "Soap McTavish",
        displayNumber = "5513458656",
        isChecked = true,
    )
    val item3 = ContactListItem(
        _id = "",
        displayName = "Kyle Garrick",
        displayNumber = "5523458656",
        isChecked = false,
    )
    val item4 = ContactListItem(
        _id = "",
        displayName = "",
        displayNumber = "44678",
        isChecked = false
    )
    Column {
        RenderContactItem(item1) {}
        RenderContactItem(item2) {}
        RenderContactItem(item3) {}
        RenderContactItem(item4) {}
    }
}