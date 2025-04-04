package balti.migrate.backup.ui.screens.listScreen.smsBackup

import android.provider.Telephony
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Drafts
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Outbox
import androidx.compose.material.icons.filled.Queue
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.SmsFailed
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import balti.migrate.R
import balti.migrate.backup.ui.screens.listScreen.ListScreenShell
import baltiapps.migrate.domain.common.model.SmsListItem
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SmsBackup(
    navigateUp: () -> Unit,
    goToNextScreen: () -> Unit,
    viewModel: SmsBackupViewModel = koinViewModel()
) {
    val lifecycleOwner = LocalLifecycleOwner.current

    val state by viewModel.state.collectAsStateWithLifecycle(
        lifecycleOwner = lifecycleOwner
    )

    Content(
        state = { state },
        navigateUp = navigateUp,
        onSelectAll = {
            viewModel.performAction(SmsBackupAction.ToggleAllSms(true))
        },
        onDeselectAll = {
            viewModel.performAction(SmsBackupAction.ToggleAllSms(false))
        },
        onItemToggled = { item ->
            viewModel.performAction(SmsBackupAction.ToggleSmsItem(item))
        },
        onNext = {
            viewModel.performAction(SmsBackupAction.StageSms(goToNextScreen))
        }
    )
}

@Composable
private fun Content(
    state: () -> SmsBackupState,
    navigateUp: () -> Unit,
    onSelectAll: () -> Unit,
    onDeselectAll: () -> Unit,
    onItemToggled: (SmsListItem) -> Unit,
    onNext: () -> Unit,
) {
    val isLoading = state().progress.percentage < 1.0
    ListScreenShell(
        backupTitle = stringResource(R.string.label_sms_backup),
        navigateUp = navigateUp,
        onSelectAll = onSelectAll,
        onDeselectAll = onDeselectAll,
        isLoading = isLoading,
        isStaging = state().isStaging,
        onNext = onNext,
    ) { paddingValues ->
        Box(
            modifier = Modifier.fillMaxSize().padding(paddingValues)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    progress = { state().progress.percentage.toFloat() },
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(
                        items = state().smsList,
                        key = { it._id }
                    ) { item ->
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
                }
            }
        }
    }
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