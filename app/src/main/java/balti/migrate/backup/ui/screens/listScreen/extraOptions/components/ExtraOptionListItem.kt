package balti.migrate.backup.ui.screens.listScreen.extraOptions.components

import androidx.compose.foundation.clickable
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun ExtraOptionListItem(
    title: String,
    description: String,
    selectedCount: Int,
    totalCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(description) },
        trailingContent = { Text("$selectedCount/$totalCount") },
        modifier = modifier.clickable(onClick = onClick),
    )
}
