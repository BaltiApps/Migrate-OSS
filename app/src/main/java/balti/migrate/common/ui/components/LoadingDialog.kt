package balti.migrate.common.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import balti.migrate.R
import baltiapps.migrate.domain.common.model.Progress

@Composable
fun LoadingDialog(
    text: String,
    progress: Progress?,
    modifier: Modifier = Modifier,
    maxLines: Int = Int.MAX_VALUE,
    onCancel: (() -> Unit)? = null,
) {
    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(
            dismissOnClickOutside = false,
            dismissOnBackPress = false,
        )
    ) {
        Surface(
            modifier = modifier
                .fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.background,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp, vertical = 16.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = text,
                        modifier = Modifier.weight(1f),
                        maxLines = maxLines
                    )
                    LoadingProgressBar(
                        progress = progress
                    )
                }
                if (onCancel != null) {
                    TextButton(
                        onClick = onCancel,
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                    ) {
                        Text(text = stringResource(android.R.string.cancel))
                    }
                }
            }
        }
    }
}

@Composable
@Preview
private fun DialogPreview1() {
    LoadingDialog(
        text = stringResource(R.string.loading),
        progress = null,
    )
}

@Composable
@Preview
private fun DialogPreview2() {
    LoadingDialog(
        text = stringResource(R.string.exporting_percentage, 40),
        progress = Progress.Empty.copy(percentage = 0.4),
    )
}