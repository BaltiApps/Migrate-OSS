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

class AppBackupSelectionViewModel(
    private val preferences: Preferences,
    private val superuserUtils: SuperuserUtils,
    private val readAppListForBackupUseCase: ReadAppListForBackupUseCase,
    private val stageSelectedApps: StageSelectedApps,
    private val backupDataRepository: BackupDataRepository,
): ViewModel() {

    private val _state = MutableStateFlow(
        AppBackupSelectionState(
            shouldSkipBackup = !preferences.wasSuperuserPermissionPreviouslyGranted()
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
                updateStatesForAllApksAllDataAllPermissions()
            }
            is AppBackupSelectionAction.ToggleAppItemDataSelection -> {
                val newItem = action.item.copy(isDataSelected = !action.item.isDataSelected)
                replaceAppItem(newItem)
                updateStatesForAllApksAllDataAllPermissions()
            }
            is AppBackupSelectionAction.ToggleAppItemPermissionSelection -> {
                val newItem = action.item.copy(isPermissionsSelected = !action.item.isPermissionsSelected)
                replaceAppItem(newItem)
                updateStatesForAllApksAllDataAllPermissions()
            }
            is AppBackupSelectionAction.ToggleEverythingForAnApp -> action.item.run {
                // select all if some are selected and deselect all if all are deselected
                val isAllSelected = this.isAllSelected()
                val newItem = copy(
                    isApkSelected = !isAllSelected,
                    isDataSelected = !isAllSelected,
                    isPermissionsSelected = !isAllSelected,
                )
                replaceAppItem(newItem)
                updateStatesForAllApksAllDataAllPermissions()
            }
            is AppBackupSelectionAction.ToggleAllAppItemsApkSelection -> {
                val newItems = state.value.appListItems.map {
                    it.copy(isApkSelected = action.isChecked)
                }
                _state.update {
                    it.copy(
                        appListItems = newItems,
                        areAllApksSelected = action.isChecked,
                    )
                }
            }
            is AppBackupSelectionAction.ToggleAllAppItemsDataSelection -> {
                val newItems = state.value.appListItems.map {
                    it.copy(isDataSelected = action.isChecked)
                }
                _state.update {
                    it.copy(
                        appListItems = newItems,
                        areAllDataSelected = action.isChecked,
                    )
                }
            }
            is AppBackupSelectionAction.ToggleAllAppItemsPermissionSelection -> {
                val newItems = state.value.appListItems.map {
                    it.copy(isPermissionsSelected = action.isChecked)
                }
                _state.update {
                    it.copy(
                        appListItems = newItems,
                        areAllPermissionsSelected = action.isChecked,
                    )
                }
            }
            is AppBackupSelectionAction.ToggleAllAppItems -> {
                val newItems = state.value.appListItems.map {
                    it.copy(
                        isApkSelected = action.isChecked,
                        isDataSelected = action.isChecked,
                        isPermissionsSelected = action.isChecked,
                    )
                }
                _state.update {
                    it.copy(
                        appListItems = newItems,
                        areAllApksSelected = action.isChecked,
                        areAllDataSelected = action.isChecked,
                        areAllPermissionsSelected = action.isChecked,
                    )
                }
            }
            is AppBackupSelectionAction.StageAppItems -> {
                _state.update { it.copy(isStaging = true) }
                stageSelectedApps.invoke(_state.value.appListItems, backupDataRepository)
                _state.update { it.copy(isStaging = false) }
                action.onStagingDone()
            }
        }
    }
}