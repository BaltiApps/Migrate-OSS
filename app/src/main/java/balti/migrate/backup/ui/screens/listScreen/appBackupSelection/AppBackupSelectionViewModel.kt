package balti.migrate.backup.ui.screens.listScreen.appBackupSelection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import balti.migrate.common.utils.SuperuserUtils
import baltiapps.migrate.domain.backup.repository.BackupDataRepository
import baltiapps.migrate.domain.backup.usecase.ReadAppListForBackupUseCase
import baltiapps.migrate.domain.common.model.AppListItem
import baltiapps.migrate.domain.common.sources.Preferences
import baltiapps.migrate.domain.common.usecase.StageSelectedApps
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AppBackupSelectionViewModel(
    private val preferences: Preferences,
    private val superuserUtils: SuperuserUtils,
    private val readAppListForBackupUseCase: ReadAppListForBackupUseCase,
    private val stageSelectedApps: StageSelectedApps,
    private val backupDataRepository: BackupDataRepository,
): ViewModel() {

    private val _state = MutableStateFlow(
        AppBackupSelectionState(
            shouldSkipBackup = !preferences.wasSuPermissionGranted()
        )
    )
    val state = _state.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch(Dispatchers.IO) {

            // If Superuser check failed, ask for superuser permission only if it was previously granted
            // and don't load anything else
            if (preferences.wasSuPermissionGranted()
                && superuserUtils.checkSuperuserPermission().isFailure) {
                _state.update {
                    it.copy(shouldAskForSuperuserPermission = true)
                    return@launch
                }
            }

            readAppListForBackupUseCase.invoke().onCompletion {
                _state.update {
                    it.copy(
                        progress = it.progress.copy(percentage = 1.0),
                        shouldAskForSuperuserPermission = false,
                        appListItems = backupDataRepository.appListItems,
                        isAllPermissionsCheckboxDisabled = backupDataRepository.appListItems.none { app -> app.isPermissionsEnabled },
                    )
                }
                updateAppListWithFilter(
                    selectionFilter = _state.value.filterSelection,
                )
                applySearchFilter()
            }.collect {
                _state.update { state ->
                    state.copy(
                        shouldAskForSuperuserPermission = false,
                        progress = it,
                    )
                }
            }
        }
    }

    private fun replaceAppItem(newItem: AppListItem) {
        _state.update {
            it.copy(
                appListItems = it.appListItems.map { if (it._id == newItem._id) newItem else it },
            )
        }
    }

    private fun updateStatesForAllApksAllDataAllPermissions() {
        _state.update {
            it.copy(
                areAllApksSelected = it.displayedAppListItems.all { it.isApkSelected },
                areAllDataSelected = it.displayedAppListItems.all { it.isDataSelected },
                areAllPermissionsSelected = it.displayedAppListItems.all { it.isPermissionsSelected || !it.isPermissionsEnabled },
            )
        }
    }

    private fun updateAppListWithFilter(selectionFilter: AppFilterSelection) {
        _state.update {
            it.copy(
                appListItems = backupDataRepository.appListItems.filter { app ->
                    (selectionFilter.systemCore && app.isSystemApp && app.isUpdatedSystemApp.not()) ||
                    (selectionFilter.systemUpdate && app.isUpdatedSystemApp) ||
                    (selectionFilter.userApps && app.isSystemApp.not() && app.isUpdatedSystemApp.not())
                }
            )
        }
        updateStatesForAllApksAllDataAllPermissions()
    }

    private fun applySearchFilter() {
        val lowerSearch = _state.value.searchText.lowercase()
        _state.update { state ->
            state.copy(
                displayedAppListItems = state.appListItems.filter {
                    lowerSearch.isBlank() ||
                            it.appName.lowercase().contains(lowerSearch) ||
                            it._id.lowercase().contains(lowerSearch) ||
                            it.versionName.lowercase().contains(lowerSearch)
                }
            )
        }
    }

    private fun toggleAll(
        apk: Boolean?,
        data: Boolean?,
        permissions: Boolean?,
    ) {
        val changedItems = state.value.displayedAppListItems.map {
            it.copy(
                isApkSelected = apk ?: it.isApkSelected,
                isDataSelected = data ?: it.isDataSelected,
                isPermissionsSelected = it.isPermissionsEnabled && permissions ?: it.isPermissionsSelected,
            )
        }
        val merged = _state.value.appListItems.map { app ->
            changedItems.find { it._id == app._id } ?: app
        }
        _state.update {
            it.copy(
                appListItems = merged,
                areAllApksSelected = apk ?: it.areAllApksSelected,
                areAllDataSelected = data ?: it.areAllDataSelected,
                areAllPermissionsSelected = permissions ?: it.areAllPermissionsSelected,
            )
        }
    }

    fun performAction(action: AppBackupSelectionAction) = viewModelScope.launch(Dispatchers.Default) {
        when(action) {
            AppBackupSelectionAction.AskSuPermission -> {
                superuserUtils.checkSuperuserPermission().run {
                    _state.update {
                        it.copy(shouldAskForSuperuserPermission = this.isFailure)
                    }
                }
            }
            is AppBackupSelectionAction.ToggleAppItemApkSelection -> {
                val newItem = action.item.copy(isApkSelected = !action.item.isApkSelected)
                replaceAppItem(newItem)
            }
            is AppBackupSelectionAction.ToggleAppItemDataSelection -> {
                val newItem = action.item.copy(isDataSelected = !action.item.isDataSelected)
                replaceAppItem(newItem)
            }
            is AppBackupSelectionAction.ToggleAppItemPermissionSelection -> {
                val newItem = action.item.copy(isPermissionsSelected = !action.item.isPermissionsSelected)
                replaceAppItem(newItem)
            }
            is AppBackupSelectionAction.ToggleEverythingForAnApp -> action.item.run {
                // select all if some are selected and deselect all if all are deselected
                val isAllSelected = this.isAllSelected()
                val newItem = copy(
                    isApkSelected = !isAllSelected,
                    isDataSelected = !isAllSelected,
                    isPermissionsSelected = action.item.isPermissionsEnabled && !isAllSelected,
                )
                replaceAppItem(newItem)
            }
            is AppBackupSelectionAction.ToggleAllAppItemsApkSelection -> {
                toggleAll(
                    apk = action.isChecked,
                    data = null,
                    permissions = null,
                )
            }
            is AppBackupSelectionAction.ToggleAllAppItemsDataSelection -> {
                toggleAll(
                    apk = null,
                    data = action.isChecked,
                    permissions = null,
                )
            }
            is AppBackupSelectionAction.ToggleAllAppItemsPermissionSelection -> {
                toggleAll(
                    apk = null,
                    data = null,
                    permissions = action.isChecked,
                )
            }
            is AppBackupSelectionAction.ToggleAllAppItems -> {
                toggleAll(
                    apk = action.isChecked,
                    data = action.isChecked,
                    permissions = action.isChecked,
                )
            }
            is AppBackupSelectionAction.StageAppItems -> {
                _state.update { it.copy(isStaging = true) }
                stageSelectedApps.invoke(_state.value.appListItems, backupDataRepository)
                _state.update { it.copy(isStaging = false) }
                withContext(Dispatchers.Main) {
                    action.onStagingDone()
                }
            }
            is AppBackupSelectionAction.UpdateFilterSelection -> {
                _state.update { it.copy(filterSelection = action.filterSelection) }
                updateAppListWithFilter(
                    selectionFilter = action.filterSelection,
                )
            }
            is AppBackupSelectionAction.UpdateSearchText -> {
                _state.update { it.copy(searchText = action.searchText) }
            }
            AppBackupSelectionAction.ToggleSearchBar -> {
                val nowVisible = !_state.value.showSearchBar
                _state.update { it.copy(showSearchBar = nowVisible) }
                if (!nowVisible) {
                    _state.update { it.copy(searchText = "") }
                }
            }
        }
        applySearchFilter()
        updateStatesForAllApksAllDataAllPermissions()
    }
}