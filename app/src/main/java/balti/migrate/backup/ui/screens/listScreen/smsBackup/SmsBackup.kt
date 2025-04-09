package balti.migrate.backup.ui.screens.listScreen.smsBackup

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
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import balti.migrate.R
import balti.migrate.common.ui.ListScreenShell
import balti.migrate.common.ui.components.LoadingProgressBar
import balti.migrate.common.ui.components.RenderSmsItem
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
                LoadingProgressBar(
                    modifier = Modifier.align(Alignment.TopCenter),
                    progress = state().progress
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(
                        items = state().smsList,
                        key = { it._id }
                    ) { item ->
                        RenderSmsItem(item, onItemToggled)
                    }
                }
            }
        }
    }
}