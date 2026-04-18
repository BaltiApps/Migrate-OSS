package balti.migrate.backup.ui.screens.listScreen.extraOptions.externalData

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import baltiapps.migrate.domain.backup.repository.BackupDataRepository
import baltiapps.migrate.domain.common.model.AppListItem
import baltiapps.migrate.domain.common.usecase.UpdateStagedApps
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ExternalDataViewModel(
    private val backupDataRepository: BackupDataRepository,
    private val updateStagedApps: UpdateStagedApps,
) : ViewModel() {

    private val _state = MutableStateFlow(ExternalDataState())
    val state = _state.asStateFlow()

    init {
        loadItems()
    }

    private fun loadItems() {
        val items = backupDataRepository.stagedApps
            .filter { it.toListItem().isDataSelected }
            .map { it.toListItem() }
        _state.update { it.copy(appListItems = items) }
        updateAllSelectedStates()
    }

    private fun replaceItem(newItem: AppListItem) {
        _state.update {
            it.copy(appListItems = it.appListItems.map { if (it._id == newItem._id) newItem else it })
        }
    }

    private fun updateAllSelectedStates() {
        _state.update {
            val items = it.appListItems
            it.copy(
                areAllExternalDataSelected = items.isNotEmpty() && items.all { it.isExternalDataSelected },
                areAllExternalMediaSelected = items.isNotEmpty() && items.all { it.isExternalMediaSelected },
            )
        }
    }

    fun performAction(action: ExternalDataAction) = viewModelScope.launch(Dispatchers.Default) {
        when (action) {
            is ExternalDataAction.ToggleExternalData -> {
                replaceItem(action.item.copy(isExternalDataSelected = !action.item.isExternalDataSelected))
                updateAllSelectedStates()
            }

            is ExternalDataAction.ToggleExternalMedia -> {
                replaceItem(action.item.copy(isExternalMediaSelected = !action.item.isExternalMediaSelected))
                updateAllSelectedStates()
            }

            is ExternalDataAction.ToggleItemBoth -> action.item.run {
                val isBothSelected = isExternalDataSelected && isExternalMediaSelected
                replaceItem(
                    copy(
                        isExternalDataSelected = !isBothSelected,
                        isExternalMediaSelected = !isBothSelected,
                    )
                )
                updateAllSelectedStates()
            }

            is ExternalDataAction.ToggleAllExternalData -> {
                val newItems =
                    _state.value.appListItems.map { it.copy(isExternalDataSelected = action.isChecked) }
                _state.update {
                    it.copy(
                        appListItems = newItems,
                        areAllExternalDataSelected = action.isChecked
                    )
                }
            }

            is ExternalDataAction.ToggleAllExternalMedia -> {
                val newItems =
                    _state.value.appListItems.map { it.copy(isExternalMediaSelected = action.isChecked) }
                _state.update {
                    it.copy(
                        appListItems = newItems,
                        areAllExternalMediaSelected = action.isChecked
                    )
                }
            }

            is ExternalDataAction.ToggleAllBoth -> {
                val newItems = _state.value.appListItems.map {
                    it.copy(
                        isExternalDataSelected = action.isChecked,
                        isExternalMediaSelected = action.isChecked
                    )
                }
                _state.update {
                    it.copy(
                        appListItems = newItems,
                        areAllExternalDataSelected = action.isChecked,
                        areAllExternalMediaSelected = action.isChecked,
                    )
                }
            }

            is ExternalDataAction.Save -> {
                // TODO
            }
        }
    }
}
