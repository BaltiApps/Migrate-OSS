package balti.migrate.restore.ui.screens.restoreSummary.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import balti.migrate.R
import balti.migrate.common.ui.components.LoadingProgressBar
import baltiapps.migrate.domain.common.model.Progress

@Composable
fun LoadingDialog(
    text: String,
    progress: Progress,
    modifier: Modifier = Modifier,
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = text,
                    modifier = Modifier.weight(1f),
                )
                LoadingProgressBar(
                    progress = progress
                )
            }
        }
    }
}

@Composable
@Preview
private fun Preview() {
    Surface(
        modifier = Modifier.fillMaxSize()
    ) {
        LoadingDialog(
            text = stringResource(R.string.exporting_percentage, 40),
            progress = Progress.Empty.copy(percentage = 0.4),
        )
    }
}