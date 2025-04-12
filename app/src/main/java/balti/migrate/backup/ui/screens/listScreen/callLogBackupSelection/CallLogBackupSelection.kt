package balti.migrate.backup.ui.screens.listScreen.callLogBackupSelection

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import balti.migrate.R
import balti.migrate.common.ui.ListScreenShell
import balti.migrate.common.ui.components.RenderCallLogItem
import baltiapps.migrate.domain.common.model.CallLogListItem
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun CallLogBackupSelection(
    navigateUp: () -> Unit,
    goToNextScreen: () -> Unit,
    viewModel: CallLogBackupSelectionViewModel = koinViewModel(),
) {
    val lifecycleOwner = LocalLifecycleOwner.current

    val state by viewModel.state.collectAsStateWithLifecycle(
        lifecycleOwner = lifecycleOwner,
    )

    Content(
        state = { state },
        navigateUp = navigateUp,
        onSelectAll = {
            viewModel.performAction(CallLogBackupSelectionAction.ToggleAllCallLog(true))
        },
        onDeselectAll = {
            viewModel.performAction(CallLogBackupSelectionAction.ToggleAllCallLog(false))
        },
        onItemToggled = {
            viewModel.performAction(CallLogBackupSelectionAction.ToggleCallLogItem(it))
        },
        onNext = {
            viewModel.performAction(CallLogBackupSelectionAction.StageCallLogs(goToNextScreen))
        }
    )
}

@Composable
private fun Content(
    state: () -> CallLogBackupSelectionState,
    navigateUp: () -> Unit,
    onSelectAll: () -> Unit,
    onDeselectAll: () -> Unit,
    onItemToggled: (CallLogListItem) -> Unit,
    onNext: () -> Unit,
) {
    val isStaging = state().isStaging
    ListScreenShell(
        backupTitle = stringResource(R.string.label_call_log_backup),
        navigateUp = navigateUp,
        onSelectAll = onSelectAll,
        onDeselectAll = onDeselectAll,
        isStaging = isStaging,
        loadingProgress = state().progress,
        onNext = onNext,
    ) { paddingValues ->
        Box(
            modifier = Modifier.fillMaxSize().padding(paddingValues)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                items(
                    items = state().callLogList,
                    key = { it._id }
                ) { item ->
                    RenderCallLogItem(
                        item = item,
                        enabled = !isStaging,
                        onItemToggled = onItemToggled,
                    )
                }
            }
        }
    }
}