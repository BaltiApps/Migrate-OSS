package balti.migrate.backup.ui.components

import android.provider.Telephony
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Drafts
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Outbox
import androidx.compose.material.icons.filled.Queue
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.SmsFailed
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
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import balti.migrate.R
import baltiapps.migrate.domain.backup.model.ItemCreationDate
import baltiapps.migrate.domain.backup.model.SmsListItem

@Composable
fun RenderSmsItem(
    item: SmsListItem,
    onItemChecked: (SmsListItem, Boolean) -> Unit
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
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(48.dp),
            contentAlignment = Alignment.Center
        ) {
            Image(
                modifier = Modifier.size(32.dp),
                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary),
                imageVector = when(item.smsType) {
                    Telephony.Sms.MESSAGE_TYPE_INBOX -> Icons.Default.Inbox
                    Telephony.Sms.MESSAGE_TYPE_OUTBOX -> Icons.Default.Outbox
                    Telephony.Sms.MESSAGE_TYPE_SENT -> Icons.Default.Check
                    Telephony.Sms.MESSAGE_TYPE_DRAFT -> Icons.Default.Drafts
                    Telephony.Sms.MESSAGE_TYPE_FAILED -> Icons.Default.SmsFailed
                    Telephony.Sms.MESSAGE_TYPE_QUEUED -> Icons.Default.Queue
                    else -> Icons.Default.Sms
                },
                contentDescription = when(item.smsType) {
                    Telephony.Sms.MESSAGE_TYPE_INBOX -> stringResource(R.string.sms_inbox)
                    Telephony.Sms.MESSAGE_TYPE_OUTBOX -> stringResource(R.string.sms_outbox)
                    Telephony.Sms.MESSAGE_TYPE_SENT -> stringResource(R.string.sms_sent)
                    Telephony.Sms.MESSAGE_TYPE_DRAFT -> stringResource(R.string.sms_draft)
                    Telephony.Sms.MESSAGE_TYPE_FAILED -> stringResource(R.string.sms_failed)
                    Telephony.Sms.MESSAGE_TYPE_QUEUED -> stringResource(R.string.sms_queued)
                    else -> stringResource(R.string.sms)
                },
            )
        }

        Column(
            modifier = Modifier
                .weight(1F, fill = true)
        ) {
            Text(
                text = item.smsAddress,
                style = TextStyle(
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            )
            Text(
                text = item.smsBody,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                style = TextStyle(
                    fontSize = 16.sp
                )
            )
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

@Preview
@Composable
private fun SmsDisplayItemPreview() {
    val item1 = SmsListItem(
        _id = "",
        smsAddress = "John Price",
        smsBody = "Bravo 6 going dark",
        creationDate = ItemCreationDate(
            dateInLong = 1L,
            displayDate = "Oct 06, 2024 - 12:00 PM",
        ),
        smsType = Telephony.Sms.MESSAGE_TYPE_INBOX,
        isChecked = true,
    )
    Column {
        RenderSmsItem(item1) { _, _ -> }
    }
}