package balti.migrate.restore.ui.screens.restoreSummary.components

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Done
import androidx.compose.material.icons.outlined.HourglassEmpty
import androidx.compose.material.icons.outlined.SaveAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import balti.migrate.common.ui.components.EmptyImageVector
import balti.migrate.common.ui.components.IconSource
import balti.migrate.restore.ui.screens.restoreSummary.RestoreSummaryItemState

@Composable
fun SummaryItem(
    @StringRes headlineStringRes: Int,
    count: Int?,
    icon: IconSource,
    state: RestoreSummaryItemState,
    modifier: Modifier = Modifier,
) {
    ListItem(
        modifier = modifier,
        headlineContent = {
            Text(
                text = count?.let { stringResource(headlineStringRes, it) } ?: stringResource(headlineStringRes),
                style = MaterialTheme.typography.titleMedium,
            )
        },
        leadingContent = {
            when (icon) {
                is IconSource.Vector -> Icon(
                    imageVector = icon.imageVector,
                    contentDescription = null,
                )
                is IconSource.Drawable -> Icon(
                    painter = icon.painter,
                    contentDescription = null,
                )
            }
        },
        trailingContent = {
            Icon(
                imageVector = when (state) {
                    RestoreSummaryItemState.WAITING -> Icons.Outlined.HourglassEmpty
                    RestoreSummaryItemState.REQUEST_USER_INPUT -> Icons.Outlined.HourglassEmpty
                    RestoreSummaryItemState.PROCESSING -> Icons.Outlined.SaveAlt
                    RestoreSummaryItemState.DONE -> Icons.Outlined.Done
                    RestoreSummaryItemState.CANCELLED -> Icons.Outlined.Close
                    RestoreSummaryItemState.ON_USER_INPUT_NEGATIVE -> Icons.Outlined.Close
                    else -> EmptyImageVector
                },
                contentDescription = null,
            )
        }
    )
}
