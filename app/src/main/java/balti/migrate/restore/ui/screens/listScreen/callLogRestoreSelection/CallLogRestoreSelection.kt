package balti.migrate.restore.ui.screens.listScreen.callLogRestoreSelection

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import balti.migrate.R
import balti.migrate.common.ui.listScreen.ListScreenShell
import balti.migrate.common.ui.components.RenderCallLogItem
import baltiapps.migrate.domain.common.model.CallLogListItem
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun CallLogRestoreSelection(
    navigateUp: () -> Unit,
    goToNextScreen: () -> Unit,
    viewModel: CallLogRestoreSelectionViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    val activity = LocalActivity.current

    Content(
        state = { state },
        navigateUp = navigateUp,
        onSelectAll = {
            viewModel.onAction(CallLogRestoreSelectionAction.ToggleAllCallLog(true))
        },
        onDeselectAll = {
            viewModel.onAction(CallLogRestoreSelectionAction.ToggleAllCallLog(false))
        },
        onItemToggled = {
            viewModel.onAction(CallLogRestoreSelectionAction.ToggleCallLogItem(it))
        },
        requestPermission = {
            viewModel.onAction(CallLogRestoreSelectionAction.RequestPermission(activity))
        },
        onNext = {
            viewModel.onAction(CallLogRestoreSelectionAction.StageCallLogs(goToNextScreen))
        }
    )
}

@Composable
private fun Content(
    state: () -> CallLogRestoreSelectionState,
    navigateUp: () -> Unit,
    onSelectAll: () -> Unit,
    onDeselectAll: () -> Unit,
    onItemToggled: (CallLogListItem) -> Unit,
    requestPermission: () -> Unit,
    onNext: () -> Unit,
) {
    val isStaging = state().isStaging
    ListScreenShell(
        backupTitle = stringResource(R.string.label_call_log_restore),
        navigateUp = navigateUp,
        onSelectAll = onSelectAll,
        onDeselectAll = onDeselectAll,
        isStaging = isStaging,
        loadingProgress = state().progress,
        isPermissionGranted = state().hasPermission,
        requestPermission = requestPermission,
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