package balti.migrate.restore.ui.screens.listScreen.smsRestoreSelection

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import balti.migrate.R
import balti.migrate.common.ui.ListScreenShell
import balti.migrate.common.ui.components.LoadingProgressBar
import balti.migrate.common.ui.components.RenderSmsItem
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
    val isStaging = state().isStaging
    ListScreenShell(
        backupTitle = stringResource(R.string.label_sms_restore),
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
                    items = state().smsList,
                    key = { it._id }
                ) { item ->
                    RenderSmsItem(
                        item = item,
                        enabled = !isStaging,
                        onItemToggled = onItemToggled,
                    )
                }
            }
        }
    }
}