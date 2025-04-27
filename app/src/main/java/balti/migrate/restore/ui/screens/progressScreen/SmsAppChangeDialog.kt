package balti.migrate.restore.ui.screens.progressScreen

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.window.DialogProperties
import balti.migrate.R

@Composable
fun SmsAppChangeDialog(
    shouldShow: Boolean,
    onAgree: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (shouldShow) {
        AlertDialog(
            modifier = modifier,
            onDismissRequest = {},
            title = {
                Text(stringResource(R.string.sms_app_change_dialog_title))
            },
            text = {
                val annotatedText = buildAnnotatedString {
                    append(stringResource(R.string.sms_app_change_dialog_message))
                    append("\n\n")
                    withStyle(
                        style = SpanStyle(fontWeight = FontWeight.Bold)
                    ) {
                        append(stringResource(R.string.this_is_mandatory))
                    }
                    append("\n\n")
                    withStyle(
                        style = SpanStyle(fontStyle = FontStyle.Italic)
                    ) {
                        append(stringResource(R.string.force_close_message))
                    }
                }
                Text(annotatedText)
            },
            confirmButton = {
                TextButton(onClick = onAgree) {
                    Text(stringResource(R.string.proceed))
                }
            },
            dismissButton = null,
            properties = DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false,
            )
        )
    }
}

@Preview
@Composable
private fun DialogPreview() {
    SmsAppChangeDialog(
        shouldShow = true,
        onAgree = {},
    )
}
