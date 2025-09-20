package balti.migrate.restore.ui.screens.listScreen.appRestoreSelection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import balti.migrate.common.utils.SuperuserUtils
import baltiapps.migrate.domain.common.model.AppListItem
import baltiapps.migrate.domain.common.sources.Preferences
import baltiapps.migrate.domain.common.usecase.StageSelectedApps
import baltiapps.migrate.domain.restore.repository.RestoreDataRepository
import baltiapps.migrate.domain.restore.usecase.ReadAppListForRestoreUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AppRestoreSelectionViewModel(
    private val preferences: Preferences,
    private val superuserUtils: SuperuserUtils,
    private val readAppListForRestoreUseCase: ReadAppListForRestoreUseCase,
    private val stageSelectedApps: StageSelectedApps,
    private val restoreDataRepository: RestoreDataRepository,
): ViewModel() {

    private val _state = MutableStateFlow(
        AppRestoreSelectionState(
            shouldSkipRestore = !preferences.wasSuperuserPermissionPreviouslyGranted()
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
            if (preferences.wasSuperuserPermissionPreviouslyGranted()
                && superuserUtils.checkSuperuserPermission().isFailure
            ) {
                _state.update {
                    it.copy(shouldAskForSuperuserPermission = true)
                    return@launch
                }
            }

            readAppListForRestoreUseCase.invoke().onCompletion {
                val appListItems = restoreDataRepository.appListItems
                _state.update {
                    it.copy(
                        progress = it.progress.copy(percentage = 1.0),
                        shouldAskForSuperuserPermission = false,
                        appListItems = appListItems,
                        shouldEnableApkSelection = appListItems.any { it.isApkEnabled },
                        shouldEnableDataSelection = appListItems.any { it.isDataEnabled },
                        shouldEnablePermissionSelection = appListItems.any { it.isPermissionsEnabled },
                    )
                }
                updateStatesForAllApksAllDataAllPermissions()
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
                areAllApksSelected = it.appListItems.all { it.isApkSelected },
                areAllDataSelected = it.appListItems.all { it.isDataSelected },
                areAllPermissionsSelected = it.appListItems.all { it.isPermissionsSelected },
            )
        }
    }

    fun performAction(action: AppRestoreSelectionAction) = viewModelScope.launch(Dispatchers.Default) {
        when(action) {
            AppRestoreSelectionAction.AskSuPermission -> {
                superuserUtils.checkSuperuserPermission().run {
                    _state.update {
                        it.copy(shouldAskForSuperuserPermission = this.isFailure)
                    }
                }
            }
            is AppRestoreSelectionAction.ToggleAppItemApkSelection -> {
                val newItem = action.item.copy(isApkSelected = !action.item.isApkSelected)
                replaceAppItem(newItem)
                updateStatesForAllApksAllDataAllPermissions()
            }
            is AppRestoreSelectionAction.ToggleAppItemDataSelection -> {
                val newItem = action.item.copy(isDataSelected = !action.item.isDataSelected)
                replaceAppItem(newItem)
                updateStatesForAllApksAllDataAllPermissions()
            }
            is AppRestoreSelectionAction.ToggleAppItemPermissionSelection -> {
                val newItem = action.item.copy(isPermissionsSelected = !action.item.isPermissionsSelected)
                replaceAppItem(newItem)
                updateStatesForAllApksAllDataAllPermissions()
            }
            is AppRestoreSelectionAction.ToggleEverythingForAnApp -> action.item.run {
                // select all if some are selected and deselect all if all are deselected
                val isAllSelected = this.isAllSelected()
                val newItem = copy(
                    isApkSelected = this.isApkEnabled && !isAllSelected,
                    isDataSelected = this.isDataEnabled && !isAllSelected,
                    isPermissionsSelected = this.isPermissionsEnabled && !isAllSelected,
                )
                replaceAppItem(newItem)
                updateStatesForAllApksAllDataAllPermissions()
            }
            is AppRestoreSelectionAction.ToggleAllAppItemsApkSelection -> {
                val newItems = state.value.appListItems.map {
                    it.copy(isApkSelected = it.isApkEnabled && action.isChecked)
                }
                _state.update {
                    it.copy(
                        appListItems = newItems,
                        areAllApksSelected = action.isChecked,
                    )
                }
            }
            is AppRestoreSelectionAction.ToggleAllAppItemsDataSelection -> {
                val newItems = state.value.appListItems.map {
                    it.copy(isDataSelected = it.isDataEnabled && action.isChecked)
                }
                _state.update {
                    it.copy(
                        appListItems = newItems,
                        areAllDataSelected = action.isChecked,
                    )
                }
            }
            is AppRestoreSelectionAction.ToggleAllAppItemsPermissionSelection -> {
                val newItems = state.value.appListItems.map {
                    it.copy(isPermissionsSelected = it.isPermissionsEnabled && action.isChecked)
                }
                _state.update {
                    it.copy(
                        appListItems = newItems,
                        areAllPermissionsSelected = action.isChecked,
                    )
                }
            }
            is AppRestoreSelectionAction.ToggleAllAppItems -> {
                val newItems = state.value.appListItems.map {
                    it.copy(
                        isApkSelected = it.isApkEnabled && action.isChecked,
                        isDataSelected = it.isDataEnabled && action.isChecked,
                        isPermissionsSelected = it.isPermissionsEnabled && action.isChecked,
                    )
                }
                _state.update {
                    it.copy(
                        appListItems = newItems,
                        areAllApksSelected = it.shouldEnableApkSelection && action.isChecked,
                        areAllDataSelected = it.shouldEnableDataSelection && action.isChecked,
                        areAllPermissionsSelected = it.shouldEnablePermissionSelection && action.isChecked,
                    )
                }
            }
            is AppRestoreSelectionAction.StageAppItems -> {
                _state.update { it.copy(isStaging = true) }
                stageSelectedApps.invoke(_state.value.appListItems, restoreDataRepository)
                _state.update { it.copy(isStaging = false) }
                withContext(Dispatchers.Main) {
                    action.onStagingDone()
                }
            }
        }
    }
}