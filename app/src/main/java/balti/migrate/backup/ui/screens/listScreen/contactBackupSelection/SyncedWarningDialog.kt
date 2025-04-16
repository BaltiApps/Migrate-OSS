package balti.migrate.backup.ui.screens.listScreen.contactBackupSelection

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import balti.migrate.R


private var shouldShowSyncedWarningDialog by mutableStateOf(false)

@Composable
fun SyncedWarningDialog() {
    if (shouldShowSyncedWarningDialog) {
        AlertDialog(
            onDismissRequest = {
                shouldShowSyncedWarningDialog = false
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        shouldShowSyncedWarningDialog = false
                    }
                ) {
                    Text(stringResource(R.string.dismiss))
                }
            },
            title = {
                Text(stringResource(R.string.synced_contacts_backup_is_not_recommended))
            },
            text = {
                Text(stringResource(R.string.synced_contacts_not_recommended_justification))
            },
        )
    }
}

fun showSyncedContactsWarningDialog() {
    shouldShowSyncedWarningDialog = true
}

@Preview
@Composable
private fun DialogPreview() {
    LaunchedEffect(Unit) {
        showSyncedContactsWarningDialog()
    }
    SyncedWarningDialog()
}