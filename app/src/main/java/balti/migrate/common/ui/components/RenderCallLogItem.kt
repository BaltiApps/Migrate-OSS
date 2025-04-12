package balti.migrate.common.ui.components

import android.provider.CallLog
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.automirrored.filled.PhoneMissed
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.SettingsPhone
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import balti.migrate.R
import baltiapps.migrate.domain.common.model.CallLogListItem
import baltiapps.migrate.domain.common.model.ItemCreationDate

@Composable
fun RenderCallLogItem(
    item: CallLogListItem,
    enabled: Boolean = true,
    onItemToggled: (CallLogListItem) -> Unit,
) {
    val alphaModifier = Modifier.alpha(
        if (enabled) 1f else 0.38f
    )
    ListItem(
        modifier = Modifier.clickable(enabled = enabled) {
            onItemToggled(item)
        },
        leadingContent = {
            CallLogIcon(
                item = item,
                modifier = alphaModifier,
            )
        },
        headlineContent = {
            Text(
                text = item.displayName.takeIf { it.isNotBlank() }
                    ?: item.displayNumber,
                modifier = alphaModifier,
            )
        },
        supportingContent = {
            Column {
                if (item.displayName.isNotBlank() && item.displayNumber.isNotBlank()) {
                    Text(
                        text = item.displayNumber,
                        modifier = alphaModifier,
                    )
                }
                Text(
                    text = item.creationDate.displayDate,
                    fontWeight = FontWeight.Thin,
                    fontSize = 12.sp
                )
            }
        },
        trailingContent = {
            Checkbox(
                checked = item.isChecked,
                onCheckedChange = null,
                enabled = enabled,
            )
        }
    )
}

@Composable
private fun CallLogIcon(
    item: CallLogListItem,
    modifier: Modifier = Modifier,
) {
    Image(
        modifier = modifier,
        imageVector = when (item.callStatus) {
            CallLog.Calls.MISSED_TYPE -> Icons.AutoMirrored.Filled.PhoneMissed
            CallLog.Calls.INCOMING_TYPE -> Icons.AutoMirrored.Filled.CallReceived
            CallLog.Calls.OUTGOING_TYPE -> Icons.AutoMirrored.Filled.CallMade
            CallLog.Calls.BLOCKED_TYPE -> Icons.Filled.Block
            CallLog.Calls.REJECTED_TYPE -> Icons.Outlined.Cancel
            CallLog.Calls.VOICEMAIL_TYPE -> ImageVector.vectorResource(R.drawable.baseline_voicemail_24)
            else -> Icons.Filled.SettingsPhone
        },
        contentDescription = when (item.callStatus) {
            CallLog.Calls.MISSED_TYPE -> stringResource(R.string.missed_call)
            CallLog.Calls.INCOMING_TYPE -> stringResource(R.string.incoming_call)
            CallLog.Calls.OUTGOING_TYPE -> stringResource(R.string.outgoing_call)
            CallLog.Calls.BLOCKED_TYPE -> stringResource(R.string.blocked_call)
            CallLog.Calls.REJECTED_TYPE -> stringResource(R.string.rejected_call)
            CallLog.Calls.VOICEMAIL_TYPE -> stringResource(R.string.voicemail)
            else -> stringResource(R.string.call)
        },
        colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary)
    )
}

@Composable
@Preview
private fun CallLogDisplayItemPreview() {
    val item1 = CallLogListItem(
        _id = "",
        callStatus = CallLog.Calls.INCOMING_TYPE,
        displayName = "John Price",
        displayNumber = "5563758676",
        isChecked = false,
        creationDate = ItemCreationDate(
            dateInLong = 1L,
            displayDate = "Oct 10, 2024 - 10:34 PM"
        )
    )
    val item2 = CallLogListItem(
        _id = "",
        callStatus = CallLog.Calls.OUTGOING_TYPE,
        displayName = "Soap McTavish",
        displayNumber = "5513458656",
        isChecked = true,
        creationDate = ItemCreationDate(
            dateInLong = 2L,
            displayDate = "Oct 11, 2024 - 8:00 PM"
        )
    )
    val item3 = CallLogListItem(
        _id = "",
        callStatus = CallLog.Calls.VOICEMAIL_TYPE,
        displayName = "Kyle Garrick",
        displayNumber = "5523458656",
        isChecked = false,
        creationDate = ItemCreationDate(
            dateInLong = 3L,
            displayDate = "Dec 11, 2023 - 7:14 AM"
        )
    )
    val item4 = CallLogListItem(
        _id = "",
        callStatus = CallLog.Calls.REJECTED_TYPE,
        displayName = "",
        displayNumber = "44678",
        isChecked = true,
        creationDate = ItemCreationDate(
            dateInLong = 4L,
            displayDate = "Jan 01, 1800 - 12:00 AM"
        )
    )
    Column {
        RenderCallLogItem(item1) {}
        RenderCallLogItem(item2, false) {}
        RenderCallLogItem(item3) {}
        RenderCallLogItem(item4) {}
    }
}
