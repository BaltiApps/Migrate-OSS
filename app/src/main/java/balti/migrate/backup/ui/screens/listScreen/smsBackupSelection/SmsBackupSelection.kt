package balti.migrate.backup.ui.screens.listScreen.smsBackupSelection

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
import balti.migrate.common.ui.components.RenderSmsItem
import balti.migrate.common.ui.listScreen.ListScreenShell
import balti.migrate.common.ui.listScreen.ListState
import balti.migrate.common.utils.PermissionUtils
import baltiapps.migrate.domain.common.model.SmsListItem
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SmsBackupSelection(
    navigateUp: () -> Unit,
    goToNextScreen: () -> Unit,
    viewModel: SmsBackupSelectionViewModel = koinViewModel()
) {
    val lifecycleOwner = LocalLifecycleOwner.current

    val state by viewModel.state.collectAsStateWithLifecycle(
        lifecycleOwner = lifecycleOwner
    )

    Content(
        state = { state },
        navigateUp = navigateUp,
        onSelectAll = {
            viewModel.performAction(SmsBackupSelectionAction.ToggleAllSms(true))
        },
        onDeselectAll = {
            viewModel.performAction(SmsBackupSelectionAction.ToggleAllSms(false))
        },
        onItemToggled = { item ->
            viewModel.performAction(SmsBackupSelectionAction.ToggleSmsItem(item))
        },
        requestPermission = PermissionUtils.requestPermission(PermissionUtils.smsReadPermission) {
            viewModel.performAction(SmsBackupSelectionAction.OnPermissionResult(it))
        },
        onNext = {
            viewModel.performAction(SmsBackupSelectionAction.StageSms(goToNextScreen))
        }
    )
}

@Composable
private fun Content(
    state: () -> SmsBackupSelectionState,
    navigateUp: () -> Unit,
    onSelectAll: () -> Unit,
    onDeselectAll: () -> Unit,
    onItemToggled: (SmsListItem) -> Unit,
    requestPermission: () -> Unit,
    onNext: () -> Unit,
) {
    val listState = ListState(
        listTitle = stringResource(R.string.label_sms_backup),
        isStaging = state().isStaging,
        hasPermission = state().hasPermission,
        permissionDescription = stringResource(R.string.sms_backup_permission_description),
        progress = state().progress,
        hasNoData = state().smsList.isEmpty(),
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
                val allItems = state().smsList
                val selectedItems = allItems.filter { it.isChecked }
                CountBar(
                    totalCount = allItems.size,
                    selectedCount = selectedItems.size,
                )
            }
            items(
                items = state().smsList,
                key = { it._id }
            ) { item ->
                RenderSmsItem(
                    item = item,
                    enabled = !listState.isStaging,
                    onItemToggled = onItemToggled,
                )
            }
        }
    }
}