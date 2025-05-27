package balti.migrate.backup.ui.screens.listScreen.callLogBackupSelection

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import balti.migrate.R
import balti.migrate.common.ui.components.CountBar
import balti.migrate.common.ui.components.RenderCallLogItem
import balti.migrate.common.ui.listScreen.ListScreenShell
import balti.migrate.common.ui.listScreen.ListState
import balti.migrate.common.utils.PermissionUtils
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
        requestPermission = PermissionUtils.requestPermissions(PermissionUtils.callLogPermissions) {
            viewModel.performAction(CallLogBackupSelectionAction.OnPermissionResult(it))
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
    requestPermission: () -> Unit,
    onItemToggled: (CallLogListItem) -> Unit,
    onNext: () -> Unit,
) {
    val listState = ListState(
        listTitle = stringResource(R.string.label_call_log_backup),
        isStaging = state().isStaging,
        hasPermission = state().hasPermission,
        permissionDescription = stringResource(R.string.call_log_backup_permission_description),
        progress = state().progress,
        hasNoData = state().callLogList.isEmpty(),
    )
    ListScreenShell(
        listState = listState,
        navigateUp = navigateUp,
        onSelectAll = onSelectAll,
        onDeselectAll = onDeselectAll,
        onPermissionRequest = requestPermission,
        onNext = onNext,
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {
            stickyHeader {
                val allItems = state().callLogList
                val selectedItems = allItems.filter { it.isChecked }
                CountBar(
                    totalCount = allItems.size,
                    selectedCount = selectedItems.size,
                )
            }
            items(
                items = state().callLogList,
                key = { it._id }
            ) { item ->
                RenderCallLogItem(
                    item = item,
                    enabled = !listState.isStaging,
                    onItemToggled = onItemToggled,
                )
            }
        }
    }
}