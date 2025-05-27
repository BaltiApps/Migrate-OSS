package balti.migrate.restore.ui.screens.restoreSummary2.components

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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import balti.migrate.common.ui.components.EmptyImageVector
import balti.migrate.restore.ui.screens.restoreSummary2.RestoreSummaryItemState

@Composable
fun SummaryItem2(
    @StringRes headlineStringRes: Int,
    count: Int?,
    icon: ImageVector,
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
            Icon(
                imageVector = icon,
                contentDescription = null,
            )
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