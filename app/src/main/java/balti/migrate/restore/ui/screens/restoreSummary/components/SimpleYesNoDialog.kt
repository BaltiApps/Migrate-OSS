package balti.migrate.restore.ui.screens.restoreSummary.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.DialogProperties
import balti.migrate.R

@Composable
fun SimpleYesNoDialog(
    dialogText: String,
    onProceed: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        onDismissRequest = {},
        modifier = modifier,
        text = {
            Text(dialogText)
        },
        confirmButton = {
            TextButton(onClick = onProceed) {
                Text(stringResource(R.string.proceed))
            }
        },
        dismissButton = {
            TextButton(onClick = onSkip) {
                Text(stringResource(R.string.skip))
            }
        },
        properties = DialogProperties(
            dismissOnClickOutside = false,
            dismissOnBackPress = false,
        )
    )
}