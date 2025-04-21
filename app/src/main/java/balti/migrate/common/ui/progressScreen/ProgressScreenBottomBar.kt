package balti.migrate.common.ui.progressScreen

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import balti.migrate.R

@Composable
fun ProgressScreenBottomBar(
    onScrollToTop: () -> Unit,
    onScrollToBottom: () -> Unit,
    modifier: Modifier = Modifier,
    fabContent: @Composable () -> Unit,
) {
    BottomAppBar(
        modifier = modifier,
        actions = {
            IconButton(
                onClick = onScrollToTop,
            ) {
                Icon(
                    imageVector = Icons.Outlined.KeyboardArrowUp,
                    contentDescription = stringResource(
                        R.string.scroll_up
                    )
                )
            }
            IconButton(
                onClick = onScrollToBottom,
            ) {
                Icon(
                    imageVector = Icons.Outlined.KeyboardArrowDown,
                    contentDescription = stringResource(
                        R.string.scroll_down
                    )
                )
            }
        },
        floatingActionButton = fabContent
    )
}