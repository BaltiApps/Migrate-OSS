package balti.migrate.backup.ui.screens.listScreen.appBackupSelection

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
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
import balti.migrate.common.ui.components.AppCountBar
import balti.migrate.common.ui.components.RenderAppListItem
import balti.migrate.common.ui.listScreen.ListScreenShell
import balti.migrate.common.ui.listScreen.ListState
import baltiapps.migrate.domain.common.model.AppListItem
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AppBackupSelection(
    navigateUp: () -> Unit,
    goToNextScreen: () -> Unit,
    viewModel: AppBackupSelectionViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Content(
        state = { state },
        navigateUp = navigateUp,
        onSelectAll = {
            viewModel.performAction(AppBackupSelectionAction.ToggleAllAppItems(true))
        },
        onDeselectAll = {
            viewModel.performAction(AppBackupSelectionAction.ToggleAllAppItems(false))
        },
        requestPermission = {
            viewModel.performAction(AppBackupSelectionAction.AskSuPermission)
        },
        onItemApkToggled = {
            viewModel.performAction(AppBackupSelectionAction.ToggleAppItemApkSelection(it))
        },
        onItemDataToggled = {
            viewModel.performAction(AppBackupSelectionAction.ToggleAppItemDataSelection(it))
        },
        onItemPermissionToggled = {
            viewModel.performAction(AppBackupSelectionAction.ToggleAppItemPermissionSelection(it))
        },
        onItemClicked = {
            viewModel.performAction(AppBackupSelectionAction.ToggleEverythingForAnApp(it))
        },
        onAllApkToggled = {
            viewModel.performAction(AppBackupSelectionAction.ToggleAllAppItemsApkSelection(it))
        },
        onAllDataToggled = {
            viewModel.performAction(AppBackupSelectionAction.ToggleAllAppItemsDataSelection(it))
        },
        onAllPermissionToggled = {
            viewModel.performAction(AppBackupSelectionAction.ToggleAllAppItemsPermissionSelection(it))
        },
        onNext = {
            viewModel.performAction(AppBackupSelectionAction.StageAppItems(goToNextScreen))
        },
    )
}

@Composable
private fun Content(
    state: () -> AppBackupSelectionState,
    navigateUp: () -> Unit,
    onSelectAll: () -> Unit,
    onDeselectAll: () -> Unit,
    requestPermission: () -> Unit,
    onItemApkToggled: (AppListItem) -> Unit,
    onItemDataToggled: (AppListItem) -> Unit,
    onItemPermissionToggled: (AppListItem) -> Unit,
    onItemClicked: (AppListItem) -> Unit,
    onAllApkToggled: (Boolean) -> Unit,
    onAllDataToggled: (Boolean) -> Unit,
    onAllPermissionToggled: (Boolean) -> Unit,
    onNext: () -> Unit,
) {
    if (state().shouldSkipBackup) {
        onNext()
        return
    }

    val listState = ListState(
        listTitle = stringResource(R.string.label_app_backup),
        isStaging = state().isStaging,
        hasPermission = !state().shouldAskForSuperuserPermission,
        permissionDescription = stringResource(R.string.app_backup_permission_description),
        progress = state().progress,
        hasNoData = state().appListItems.isEmpty()
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
                val allItems = state().appListItems
                val selectedItems = allItems.filter { it.isAnySelected() }
                Column(modifier = Modifier.fillMaxWidth()) {
                    AppCountBar(
                        totalCount = allItems.size,
                        selectedCount = selectedItems.size,
                        isStaging = listState.isStaging,
                        areAllApksSelected = state().areAllApksSelected,
                        areAllDataSelected = state().areAllDataSelected,
                        areAllPermissionsSelected = state().areAllPermissionsSelected,
                        onAllApkToggled = onAllApkToggled,
                        onAllDataToggled = onAllDataToggled,
                        onAllPermissionToggled = onAllPermissionToggled,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceContainer)
                            .padding(8.dp),
                    )
                }
            }
            items(
                items = state().appListItems,
                key = { it._id }
            ) { item ->
                RenderAppListItem(
                    item = item,
                    enabled = !listState.isStaging,
                    onApkSelected = onItemApkToggled,
                    onDataSelected = onItemDataToggled,
                    onPermissionSelected = onItemPermissionToggled,
                    onItemClicked = onItemClicked,
                )
            }
        }
    }
}