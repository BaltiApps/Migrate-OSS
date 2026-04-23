package balti.migrate.backup.ui.screens.listScreen.appBackupSelection

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import balti.migrate.R
import balti.migrate.backup.ui.screens.listScreen.appBackupSelection.components.AppSelectionFilterDialog
import balti.migrate.common.ui.components.AppCountBar
import balti.migrate.common.ui.components.RenderAppListItem
import balti.migrate.common.ui.components.SearchBar
import balti.migrate.common.ui.listScreen.ListScreenShell
import balti.migrate.common.ui.listScreen.ListState
import baltiapps.migrate.domain.common.model.AppListItem
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AppBackupSelection(
    navigateUp: () -> Unit,
    skipAndGoToNextScreen: () -> Unit,
    goToNextScreen: () -> Unit,
    viewModel: AppBackupSelectionViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    BackHandler(enabled = state.showSearchBar) {
        viewModel.performAction(AppBackupSelectionAction.ToggleSearchBar)
    }
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
        onFilterChanged = {
            viewModel.performAction(AppBackupSelectionAction.UpdateFilterSelection(it))
        },
        onSearchTextChanged = {
            viewModel.performAction(AppBackupSelectionAction.UpdateSearchText(it))
        },
        onToggleSearchBar = {
            viewModel.performAction(AppBackupSelectionAction.ToggleSearchBar)
        },
        skipAndGoToNextScreen = skipAndGoToNextScreen,
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
    onFilterChanged: (AppFilterSelection) -> Unit,
    onSearchTextChanged: (String) -> Unit,
    onToggleSearchBar: () -> Unit,
    skipAndGoToNextScreen: () -> Unit,
    onNext: () -> Unit,
) {
    if (state().shouldSkipBackup) {
        skipAndGoToNextScreen()
        return
    }

    var showFilterDialog by remember { mutableStateOf(false) }

    if (showFilterDialog) {
        AppSelectionFilterDialog(
            initialSelection = state().filterSelection,
            onConfirm = {
                onFilterChanged(it)
                showFilterDialog = false
            },
            onDismiss = { showFilterDialog = false }
        )
    }

    val listState = ListState(
        listTitle = stringResource(R.string.label_app_backup),
        isStaging = state().isStaging,
        hasPermission = !state().shouldAskForSuperuserPermission,
        permissionDescription = stringResource(R.string.app_backup_permission_description),
        progress = state().progress,
        hasNoData = state().displayedAppListItems.isEmpty(),
        customNoDataMessage = if (state().showSearchBar) stringResource(R.string.no_app_found) else null,
        customNoDataDescription = if (state().showSearchBar) "" else null,
    )

    ListScreenShell(
        listState = listState,
        navigateUp = navigateUp,
        onSelectAll = onSelectAll,
        onDeselectAll = onDeselectAll,
        onPermissionRequest = requestPermission,
        onNext = if (state().showSearchBar) onToggleSearchBar else onNext,
        nextButtonCustomLabel = if (state().showSearchBar) stringResource(R.string.done) else null,
        footer = {
            AnimatedVisibility(visible = state().showSearchBar) {
                SearchBar(
                    searchText = state().searchText,
                    onSearchTextChanged = onSearchTextChanged,
                    onDone = onToggleSearchBar,
                )
            }
        },
        topBarActions = {
            IconButton(onClick = { showFilterDialog = true }) {
                Icon(imageVector = Icons.Default.FilterList, contentDescription = null)
            }
            IconButton(onClick = onToggleSearchBar) {
                Icon(
                    imageVector = if (state().showSearchBar) Icons.Default.Close else Icons.Default.Search,
                    contentDescription = null,
                )
            }
        }
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {
            stickyHeader {
                val allItems = state().displayedAppListItems
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
                        shouldEnablePermissionSelection = !state().isAllPermissionsCheckboxDisabled,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceContainer)
                            .padding(8.dp),
                    )
                }
            }
            items(
                items = state().displayedAppListItems,
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