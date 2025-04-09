package balti.migrate.common.ui.components

import android.provider.Telephony
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Drafts
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Outbox
import androidx.compose.material.icons.filled.Queue
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.SmsFailed
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import balti.migrate.R
import baltiapps.migrate.domain.common.model.ItemCreationDate
import baltiapps.migrate.domain.common.model.SmsListItem

@Composable
fun RenderSmsItem(
    item: SmsListItem,
    onItemToggled: (SmsListItem) -> Unit
) {
    ListItem(
        modifier = Modifier.clickable {
            onItemToggled(item)
        },
        leadingContent = {
            SmsListItemIcon(item)
        },
        headlineContent = {
            Text(text = item.smsAddress)
        },
        supportingContent = {
            Column {
                Text(
                    text = item.smsBody,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
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
            )
        }
    )
}

@Composable
private fun SmsListItemIcon(item: SmsListItem) {
    Image(
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
        RenderSmsItem(item1) {}
    }
}