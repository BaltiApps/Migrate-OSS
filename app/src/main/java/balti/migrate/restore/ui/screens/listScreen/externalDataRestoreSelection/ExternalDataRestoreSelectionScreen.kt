package balti.migrate.restore.ui.screens.listScreen.externalDataRestoreSelection

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import balti.migrate.R
import balti.migrate.common.ui.components.ExternalDataAppRow
import balti.migrate.common.ui.components.ExternalDataCountBar
import balti.migrate.common.ui.listScreen.ListScreenShell
import balti.migrate.common.ui.listScreen.ListState
import baltiapps.migrate.domain.common.model.AppListItem
import baltiapps.migrate.domain.common.model.Progress
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ExternalDataRestoreSelectionScreen(
    navigateUp: () -> Unit,
    goToNextScreen: () -> Unit,
    viewModel: ExternalDataRestoreSelectionViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Content(
        state = { state },
        navigateUp = navigateUp,
        onSelectAll = {
            viewModel.performAction(
                ExternalDataRestoreSelectionAction.ToggleAllBoth(true)
            )
        },
        onDeselectAll = {
            viewModel.performAction(
                ExternalDataRestoreSelectionAction.ToggleAllBoth(false)
            )
        },
        onExternalDataToggled = {
            viewModel.performAction(
                ExternalDataRestoreSelectionAction.ToggleExternalData(it)
            )
        },
        onExternalMediaToggled = {
            viewModel.performAction(
                ExternalDataRestoreSelectionAction.ToggleExternalMedia(it)
            )
        },
        onAllExternalDataToggled = {
            viewModel.performAction(ExternalDataRestoreSelectionAction.ToggleAllExternalData(it))
        },
        onAllExternalMediaToggled = {
            viewModel.performAction(ExternalDataRestoreSelectionAction.ToggleAllExternalMedia(it))
        },
        onRowClicked = {
            viewModel.performAction(
                ExternalDataRestoreSelectionAction.ToggleItemBoth(it)
            )
        },
        onNext = { viewModel.performAction(ExternalDataRestoreSelectionAction.Save(goToNextScreen)) },
        onSkip = goToNextScreen,
    )

    BackHandler { navigateUp() }
}

@Composable
private fun Content(
    state: () -> ExternalDataRestoreSelectionState,
    navigateUp: () -> Unit,
    onSelectAll: () -> Unit,
    onDeselectAll: () -> Unit,
    onExternalDataToggled: (AppListItem) -> Unit,
    onExternalMediaToggled: (AppListItem) -> Unit,
    onAllExternalDataToggled: (Boolean) -> Unit,
    onAllExternalMediaToggled: (Boolean) -> Unit,
    onRowClicked: (AppListItem) -> Unit,
    onNext: () -> Unit,
    onSkip: () -> Unit,
) {
    val listState = ListState(
        listTitle = stringResource(R.string.external_data),
        isStaging = state().isStaging,
        hasPermission = true,
        permissionDescription = "",
        progress = Progress.Empty.copy(percentage = 1.0),
        hasNoData = state().appListItems.isEmpty(),
    )
    ListScreenShell(
        listState = listState,
        navigateUp = navigateUp,
        onSelectAll = onSelectAll,
        onDeselectAll = onDeselectAll,
        onPermissionRequest = {},
        onNext = onNext,
        onSkip = onSkip,
    ) {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            stickyHeader {
                ExternalDataCountBar(
                    totalCount = state().appListItems.size,
                    selectedCount = state().appListItems.count {
                        it.isExternalDataSelected || it.isExternalMediaSelected
                    },
                    isStaging = listState.isStaging,
                    areAllExternalDataSelected = state().areAllExternalDataSelected,
                    areAllExternalMediaSelected = state().areAllExternalMediaSelected,
                    onAllExternalDataToggled = onAllExternalDataToggled,
                    onAllExternalMediaToggled = onAllExternalMediaToggled,
                    shouldEnableExternalDataSelection = state().shouldEnableExternalDataSelection,
                    shouldEnableExternalMediaSelection = state().shouldEnableExternalMediaSelection,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                        .padding(8.dp),
                )
            }
            items(
                items = state().appListItems,
                key = { it._id },
            ) { item ->
                ExternalDataAppRow(
                    item = item,
                    enabled = !listState.isStaging,
                    onExternalDataToggled = onExternalDataToggled,
                    onExternalMediaToggled = onExternalMediaToggled,
                    onRowClicked = onRowClicked,
                )
            }
        }
    }
}
