package balti.migrate.restore.ui.screens.listScreen.smsRestoreSelection

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import balti.migrate.R
import balti.migrate.common.ui.components.CountBar
import balti.migrate.common.ui.components.RenderSmsItem
import balti.migrate.common.ui.listScreen.ListScreenShell
import balti.migrate.common.ui.listScreen.ListState
import baltiapps.migrate.domain.common.model.SmsListItem
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SmsRestoreSelection(
    navigateUp: () -> Unit,
    goToNextScreen: () -> Unit,
    viewModel: SmsRestoreSelectionViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Content(
        state = { state },
        navigateUp = navigateUp,
        onSelectAll = {
            viewModel.onAction(SmsRestoreSelectionAction.ToggleAllSms(true))
        },
        onDeselectAll = {
            viewModel.onAction(SmsRestoreSelectionAction.ToggleAllSms(false))
        },
        onItemToggled = {
            viewModel.onAction(SmsRestoreSelectionAction.ToggleSmsItem(it))
        },
        onNext = {
            viewModel.onAction(SmsRestoreSelectionAction.StageSms(goToNextScreen))
        }
    )

    BackHandler { navigateUp() }
}

@Composable
private fun Content(
    state: () -> SmsRestoreSelectionState,
    navigateUp: () -> Unit,
    onSelectAll: () -> Unit,
    onDeselectAll: () -> Unit,
    onItemToggled: (SmsListItem) -> Unit,
    onNext: () -> Unit,
) {
    val listState = ListState(
        listTitle = stringResource(R.string.label_sms_restore),
        isStaging = state().isStaging,
        hasPermission = true,
        permissionDescription = "",
        progress = state().progress,
        hasNoData = state().smsList.isEmpty(),
    )
    ListScreenShell(
        listState = listState,
        navigateUp = navigateUp,
        onSelectAll = onSelectAll,
        onDeselectAll = onDeselectAll,
        onPermissionRequest = {},
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