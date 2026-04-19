package balti.migrate.restore.ui.screens.listScreen.externalDataRestoreSelection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import baltiapps.migrate.domain.common.model.AppListItem
import baltiapps.migrate.domain.common.usecase.UpdateStagedApps
import baltiapps.migrate.domain.restore.repository.RestoreDataRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ExternalDataRestoreSelectionViewModel(
    private val restoreDataRepository: RestoreDataRepository,
    private val updateStagedApps: UpdateStagedApps,
) : ViewModel() {

    private val _state = MutableStateFlow(ExternalDataRestoreSelectionState())
    val state = _state.asStateFlow()

    init {
        loadItems()
    }

    private fun loadItems() {
        val items = restoreDataRepository.stagedApps
            .filter { it.toListItem().isExternalDataSelected || it.toListItem().isExternalMediaSelected }
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

    fun performAction(action: ExternalDataRestoreSelectionAction) = viewModelScope.launch(Dispatchers.Default) {
        when (action) {
            is ExternalDataRestoreSelectionAction.ToggleExternalData -> {
                replaceItem(action.item.copy(isExternalDataSelected = !action.item.isExternalDataSelected))
                updateAllSelectedStates()
            }

            is ExternalDataRestoreSelectionAction.ToggleExternalMedia -> {
                replaceItem(action.item.copy(isExternalMediaSelected = !action.item.isExternalMediaSelected))
                updateAllSelectedStates()
            }

            is ExternalDataRestoreSelectionAction.ToggleItemBoth -> action.item.run {
                val isBothSelected = isExternalDataSelected && isExternalMediaSelected
                replaceItem(
                    copy(
                        isExternalDataSelected = !isBothSelected,
                        isExternalMediaSelected = !isBothSelected,
                    )
                )
                updateAllSelectedStates()
            }

            is ExternalDataRestoreSelectionAction.ToggleAllExternalData -> {
                val newItems =
                    _state.value.appListItems.map { it.copy(isExternalDataSelected = action.isChecked) }
                _state.update {
                    it.copy(
                        appListItems = newItems,
                        areAllExternalDataSelected = action.isChecked
                    )
                }
            }

            is ExternalDataRestoreSelectionAction.ToggleAllExternalMedia -> {
                val newItems =
                    _state.value.appListItems.map { it.copy(isExternalMediaSelected = action.isChecked) }
                _state.update {
                    it.copy(
                        appListItems = newItems,
                        areAllExternalMediaSelected = action.isChecked
                    )
                }
            }

            is ExternalDataRestoreSelectionAction.ToggleAllBoth -> {
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

            is ExternalDataRestoreSelectionAction.Save -> {
                _state.update { it.copy(isStaging = true) }
                updateStagedApps.invoke(_state.value.appListItems, restoreDataRepository)
                _state.update { it.copy(isStaging = false) }
                withContext(Dispatchers.Main) { action.onSaved() }
            }
        }
    }
}
