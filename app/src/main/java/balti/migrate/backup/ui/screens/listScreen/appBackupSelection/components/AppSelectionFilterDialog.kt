package balti.migrate.backup.ui.screens.listScreen.appBackupSelection.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import balti.migrate.R
import balti.migrate.app.ui.theme.SystemCoreColor
import balti.migrate.app.ui.theme.SystemUpdatedColor
import balti.migrate.backup.ui.screens.listScreen.appBackupSelection.AppFilterSelection

@Composable
fun AppSelectionFilterDialog(
    initialSelection: AppFilterSelection,
    onConfirm: (AppFilterSelection) -> Unit,
    onDismiss: () -> Unit,
) {
    var selection by remember { mutableStateOf(initialSelection) }

    AlertDialog(
        onDismissRequest = onDismiss,
        text = {
            Column {
                FilterItem(
                    label = stringResource(R.string.system_core),
                    labelColor = SystemCoreColor,
                    isSelected = selection.systemCore,
                    onClick = { selection = selection.copy(systemCore = !selection.systemCore) }
                )
                FilterItem(
                    label = stringResource(R.string.system_updated),
                    labelColor = SystemUpdatedColor,
                    isSelected = selection.systemUpdate,
                    onClick = { selection = selection.copy(systemUpdate = !selection.systemUpdate) }
                )
                FilterItem(
                    label = stringResource(R.string.user_apps),
                    isSelected = selection.userApps,
                    onClick = { selection = selection.copy(userApps = !selection.userApps) }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(selection) }) {
                Text(text = stringResource(android.R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
private fun FilterItem(
    label: String,
    isSelected: Boolean,
    labelColor: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit,
) {
    ListItem(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        headlineContent = {
            Text(
                text = label,
                color = labelColor
            )
        },
        trailingContent = {
            Checkbox(
                checked = isSelected,
                onCheckedChange = null
            )
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}
