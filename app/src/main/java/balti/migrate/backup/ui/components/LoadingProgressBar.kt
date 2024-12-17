package balti.migrate.backup.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import baltiapps.migrate.domain.backup.model.Progress

@Composable
fun LoadingProgressBar(
    modifier: Modifier = Modifier,
    progress: Progress,
) {
    Box(modifier = modifier.fillMaxWidth()) {
        CircularProgressIndicator(
            modifier = Modifier.align(Alignment.Center),
            progress = {
                progress.percentage.toFloat()
            },
        )
    }
}