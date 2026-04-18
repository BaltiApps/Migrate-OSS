package balti.migrate.backup.ui.screens.listScreen.extraOptions

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import balti.migrate.R
import balti.migrate.backup.ui.screens.listScreen.extraOptions.components.ExtraOptionListItem
import balti.migrate.common.ui.listScreen.ListScreenShell
import balti.migrate.common.ui.listScreen.ListState
import balti.migrate.common.utils.ObserveLifecycle
import baltiapps.migrate.domain.common.model.Progress
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ExtraOptionsScreen(
    navigateUp: () -> Unit,
    goToNextScreen: () -> Unit,
    goToExternalDataScreen: () -> Unit,
    viewModel: ExtraOptionsViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ObserveLifecycle { event ->
        if (event == Lifecycle.Event.ON_RESUME) {
            viewModel.performAction(ExtraOptionsAction.RefreshCounts)
        }
    }

    Content(
        state = { state },
        navigateUp = navigateUp,
        goToNextScreen = goToNextScreen,
        goToExternalDataScreen = goToExternalDataScreen,
    )
}

@Composable
private fun Content(
    state: () -> ExtraOptionsState,
    navigateUp: () -> Unit,
    goToNextScreen: () -> Unit,
    goToExternalDataScreen: () -> Unit,
) {
    val listState = ListState(
        listTitle = stringResource(R.string.extra_options),
        isStaging = state().isLoading,
        hasPermission = true,
        permissionDescription = "",
        progress = Progress.Empty.copy(percentage = 1.0),
        hasNoData = state().externalDataTotalCount == 0,
    )
    ListScreenShell(
        listState = listState,
        navigateUp = navigateUp,
        onSelectAll = null,
        onDeselectAll = null,
        onPermissionRequest = {},
        onNext = goToNextScreen,
    ) {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item {
                ExtraOptionListItem(
                    title = stringResource(R.string.external_data),
                    description = stringResource(R.string.external_data_description),
                    selectedCount = state().externalDataSelectedCount,
                    totalCount = state().externalDataTotalCount,
                    onClick = goToExternalDataScreen,
                )
            }
        }
    }
}
