package balti.migrate.backup.ui.screens.listScreen.callLogBackup

import android.provider.CallLog
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.automirrored.filled.PhoneMissed
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.SettingsPhone
import androidx.compose.material.icons.outlined.Cancel
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import balti.migrate.R
import baltiapps.migrate.domain.backup.model.CallLogListItem
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun CallLogBackup(
    isLoading: (Boolean) -> Unit,
    setStagingBlock: (() -> Unit) -> Unit,
    onPostStaging: () -> Unit,
    setToggleAll: ((Boolean) -> Unit) -> Unit,
    viewModel: CallLogBackupViewModel = koinViewModel(),
) {
    setStagingBlock {
        viewModel.performAction(CallLogBackupAction.StageCallLogs(onPostStaging))
    }
    setToggleAll {
        viewModel.performAction(CallLogBackupAction.ToggleAllCallLog(it))
    }
    val lifecycleOwner = LocalLifecycleOwner.current

    val state by viewModel.state.collectAsStateWithLifecycle(
        lifecycleOwner = lifecycleOwner,
    )

    Content(
        state = { state },
        isLoading = isLoading,
        onItemToggled = { item ->
            viewModel.performAction(CallLogBackupAction.ToggleCallLogItem(item))
        }
    )
}

@Composable
private fun Content(
    state: () -> CallLogBackupState,
    isLoading: (Boolean) -> Unit,
    onItemToggled: (CallLogListItem) -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        if (state().progress.percentage < 1.0) {
            isLoading(true)
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                progress = { state().progress.percentage.toFloat() },
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                items(
                    items = state().callLogList,
                    key = { it._id }
                ) { item ->
                    ListItem(
                        modifier = Modifier.clickable {
                            onItemToggled(item)
                        },
                        leadingContent = {
                            CallLogIcon(item)
                        },
                        headlineContent = {
                            Text(
                                text = item.displayName.takeIf { it.isNotBlank() }
                                    ?: item.displayNumber
                            )
                        },
                        supportingContent = {
                            Column {
                                if (item.displayName.isNotBlank() && item.displayNumber.isNotBlank()) {
                                    Text(item.displayNumber)
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
                            )
                        }
                    )
                }
            }
            isLoading(false)
        }
    }
}

@Composable
private fun CallLogIcon(
    item: CallLogListItem
) {
    Image(
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