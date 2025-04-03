package balti.migrate.backup.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import baltiapps.migrate.domain.common.model.Progress

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