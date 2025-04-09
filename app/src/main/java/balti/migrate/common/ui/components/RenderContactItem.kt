package balti.migrate.common.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import balti.migrate.R
import baltiapps.migrate.domain.common.model.ContactListItem

@Composable
fun RenderContactItem(
    item: ContactListItem,
    onItemChecked: (ContactListItem, Boolean) -> Unit
) {
    val contentPadding = dimensionResource(R.dimen.standard_padding)
    var isChecked by remember { mutableStateOf(item.isChecked) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                isChecked = !isChecked
                onItemChecked(item, isChecked)
            }
            .padding(contentPadding),
        horizontalArrangement = Arrangement.spacedBy(contentPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(48.dp),
            contentAlignment = Alignment.Center
        ) {
            Image(
                modifier = Modifier.size(32.dp),
                imageVector = Icons.Filled.AccountCircle,
                contentDescription = null,
                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary)
            )
        }

        Column(
            modifier = Modifier
                .weight(1F, fill = true)
        ) {
            Text(
                text = item.displayName.takeIf { it.isNotBlank() } ?: item.displayNumber,
                style = TextStyle(
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            )
            if (
                item.displayNumber.isNotBlank() &&
                item.displayName.isNotBlank() &&
                item.displayName != item.displayNumber
            ) {
                Text(
                    text = item.displayNumber,
                    style = TextStyle(
                        fontSize = 16.sp
                    )
                )
            }
        }
        Checkbox(
            modifier = Modifier.padding(
                horizontal = contentPadding
            ),
            checked = isChecked,
            onCheckedChange = null,
        )
    }
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
        RenderContactItem(item1) { _, _ -> }
        RenderContactItem(item2) { _, _ -> }
        RenderContactItem(item3) { _, _ -> }
        RenderContactItem(item4) { _, _ -> }
    }
}