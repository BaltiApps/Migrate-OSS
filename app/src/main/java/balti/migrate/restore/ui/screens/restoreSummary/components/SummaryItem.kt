package balti.migrate.restore.ui.screens.restoreSummary.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Done
import androidx.compose.material.icons.outlined.HourglassEmpty
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import balti.migrate.common.ui.components.EmptyImageVector
import balti.migrate.restore.ui.screens.restoreSummary.UserActionState


@Composable
fun SummaryItem(
    count: Int,
    labelRes: Int,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    userActionState: UserActionState = UserActionState.NOT_APPLICABLE,
) {
    if (count > 0) {
        SummaryItem(
            headline = stringResource(labelRes, count),
            icon = icon,
            modifier = modifier,
            userActionState = userActionState
        )
    }
}

@Composable
fun SummaryItem(
    headline: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    userActionState: UserActionState = UserActionState.NOT_APPLICABLE,
) {
    ListItem(
        modifier = modifier,
        headlineContent = {
            Text(
                text = headline,
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
                imageVector = when (userActionState) {
                    UserActionState.ACTION_AWAITING -> Icons.Outlined.HourglassEmpty
                    UserActionState.ACTION_PROMPT -> Icons.Outlined.HourglassEmpty
                    UserActionState.ACTION_PROCEED -> Icons.Outlined.Done
                    UserActionState.ACTION_CANCELLED -> Icons.Outlined.Close
                    UserActionState.ACTION_NO_ACTION_NEEDED -> Icons.Outlined.Done
                    else -> EmptyImageVector
                },
                contentDescription = null,
            )
        }
    )
}