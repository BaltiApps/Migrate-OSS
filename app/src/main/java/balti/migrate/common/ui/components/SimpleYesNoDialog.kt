package balti.migrate.common.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.DialogProperties
import balti.migrate.R

@Composable
fun SimpleYesNoDialog(
    dialogText: String,
    onPositiveButton: () -> Unit,
    modifier: Modifier = Modifier,
    onNegativeButton: (() -> Unit)? = null,
    positiveButtonLabel: String? = null,
    negativeButtonLabel: String? = null,
    icon: ImageVector? = null,
    iconTint: Color? = null,
    titleText: String? = null,
) {
    AlertDialog(
        onDismissRequest = {},
        modifier = modifier,
        text = {
            Text(dialogText)
        },
        title = titleText?.run {
            { Text(titleText) }
        },
        confirmButton = {
            TextButton(onClick = onPositiveButton) {
                Text(positiveButtonLabel ?: stringResource(R.string.proceed))
            }
        },
        dismissButton = onNegativeButton?.let {
            {
                TextButton(onClick = it) {
                    Text(negativeButtonLabel ?: stringResource(R.string.skip))
                }
            }
        },
        icon = if (icon != null) {
            {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint ?: LocalContentColor.current,
                )
            }
        } else null,
        properties = DialogProperties(
            dismissOnClickOutside = false,
            dismissOnBackPress = false,
        )
    )
}