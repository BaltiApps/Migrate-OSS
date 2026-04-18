package balti.migrate.backup.ui.screens.listScreen.extraOptions.externalData

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
fun ExternalDataScreen(
    navigateOnSave: () -> Unit,
    viewModel: ExternalDataViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Content(
        state = { state },
        navigateOnSave = navigateOnSave,
        onSelectAll = { viewModel.performAction(ExternalDataAction.ToggleAllBoth(true)) },
        onDeselectAll = { viewModel.performAction(ExternalDataAction.ToggleAllBoth(false)) },
        onExternalDataToggled = { viewModel.performAction(ExternalDataAction.ToggleExternalData(it)) },
        onExternalMediaToggled = { viewModel.performAction(ExternalDataAction.ToggleExternalMedia(it)) },
        onAllExternalDataToggled = {
            viewModel.performAction(
                ExternalDataAction.ToggleAllExternalData(it)
            )
        },
        onAllExternalMediaToggled = {
            viewModel.performAction(
                ExternalDataAction.ToggleAllExternalMedia(it)
            )
        },
        onRowClicked = { viewModel.performAction(ExternalDataAction.ToggleItemBoth(it)) },
        onSave = { viewModel.performAction(ExternalDataAction.Save(navigateOnSave)) },
    )
}

@Composable
private fun Content(
    state: () -> ExternalDataState,
    navigateOnSave: () -> Unit,
    onSelectAll: () -> Unit,
    onDeselectAll: () -> Unit,
    onExternalDataToggled: (AppListItem) -> Unit,
    onExternalMediaToggled: (AppListItem) -> Unit,
    onAllExternalDataToggled: (Boolean) -> Unit,
    onAllExternalMediaToggled: (Boolean) -> Unit,
    onRowClicked: (AppListItem) -> Unit,
    onSave: () -> Unit,
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
        navigateUp = navigateOnSave,
        onSelectAll = onSelectAll,
        onDeselectAll = onDeselectAll,
        onPermissionRequest = {},
        onNext = onSave,
        nextButtonCustomLabel = stringResource(R.string.save),
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
