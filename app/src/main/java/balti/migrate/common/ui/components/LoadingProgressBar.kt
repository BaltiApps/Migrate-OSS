package balti.migrate.common.ui.components

import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import baltiapps.migrate.domain.common.model.Progress

@Composable
fun LoadingProgressBar(
    modifier: Modifier = Modifier,
    progress: Progress,
) {
    CircularProgressIndicator(
        modifier = modifier,
        progress = {
            progress.percentage.toFloat()
        },
    )
}