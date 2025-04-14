package balti.migrate.common.ui.listScreen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import balti.migrate.common.ui.components.LoadingProgressBar
import baltiapps.migrate.domain.common.model.Progress

@Composable
fun ListLoadingLayout(
    progress: Progress,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize(),
    ) {
        LoadingProgressBar(
            modifier = Modifier.align(Alignment.Center),
            progress = progress,
        )
    }
}