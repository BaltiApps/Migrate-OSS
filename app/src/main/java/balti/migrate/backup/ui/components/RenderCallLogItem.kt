package balti.migrate.backup.ui.components

import android.provider.CallLog
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.automirrored.filled.PhoneMissed
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.SettingsPhone
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import balti.migrate.R
import baltiapps.migrate.domain.common.model.CallLogListItem
import baltiapps.migrate.domain.common.model.ItemCreationDate

@Composable
fun RenderCallLogItem(
    item: CallLogListItem,
    onItemChecked: (CallLogListItem, Boolean) -> Unit
) {
    val contentPadding = dimensionResource(R.dimen.standard_padding)
    var isChecked by remember { mutableStateOf(item.isChecked) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                isChecked = !isChecked
                onItemChecked(item, isChecked)
            }
            .padding(contentPadding),
        horizontalArrangement = Arrangement.spacedBy(contentPadding),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(48.dp),
            contentAlignment = Alignment.Center
        ) {
            Image(
                modifier = Modifier.size(32.dp),
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
        Column(
            modifier = Modifier
                .weight(1F, fill = true)
        ) {
            Text(
                text = item.displayName.takeIf { it.isNotBlank() } ?: item.displayNumber,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
            if (
                item.displayNumber.isNotBlank() &&
                item.displayName.isNotBlank() &&
                item.displayName != item.displayNumber
            ) {
                Text(
                    text = item.displayNumber,
                    fontSize = 16.sp
                )
            }
            Text(
                text = item.creationDate.displayDate,
                fontWeight = FontWeight.Thin,
                fontSize = 16.sp,
            )
        }
        Checkbox(
            modifier = Modifier.padding(
                horizontal = contentPadding
            ),
            checked = isChecked,
            onCheckedChange = null,
        )
    }
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
        RenderCallLogItem(item1) { _, _ -> }
        RenderCallLogItem(item2) { _, _ -> }
        RenderCallLogItem(item3) { _, _ -> }
        RenderCallLogItem(item4) { _, _ -> }
    }
}
