package balti.migrate.restore.ui.screens.listScreen.contactRestoreSelection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import balti.migrate.common.utils.ListItemUtils
import baltiapps.migrate.domain.common.usecase.StageSelectedCallLogs
import baltiapps.migrate.domain.common.usecase.StageSelectedContacts
import baltiapps.migrate.domain.restore.repository.RestoreDataRepository
import baltiapps.migrate.domain.restore.usecase.ReadContactsForRestoreUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ContactRestoreSelectionViewModel(
    private val listItemUtils: ListItemUtils,
    private val dataRepository: RestoreDataRepository,
    private val readContactsForRestoreUseCase: ReadContactsForRestoreUseCase,
    private val stageSelectedCallLogs: StageSelectedContacts,
) : ViewModel() {

    private val _state = MutableStateFlow(ContactRestoreSelectionState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            readContactsForRestoreUseCase.invoke().onCompletion {
                _state.update {
                    it.copy(
                        progress = it.progress.copy(percentage = 1.0),
                        hasPermission = true,
                        contactListItems = dataRepository.contactsListItems
                    )
                }
            }.collect {
                _state.update { state ->
                    state.copy(
                        hasPermission = true,
                        progress = it
                    )
                }
            }
        }
    }

    fun onAction(action: ContactRestoreSelectionAction) = viewModelScope.launch {
        when(action) {
            is ContactRestoreSelectionAction.ToggleContactItem -> {
                val result = listItemUtils.toggleSingleItem(_state.value.contactListItems, action.item)
                _state.update { it.copy(contactListItems = result) }
            }
            is ContactRestoreSelectionAction.ToggleAllContacts -> {
                val result = listItemUtils.toggleAllItems(_state.value.contactListItems, action.isChecked)
                _state.update { it.copy(contactListItems = result) }
            }
            is ContactRestoreSelectionAction.StageContacts -> {
                _state.update { it.copy(isStaging = true) }
                stageSelectedCallLogs.invoke(
                    allListItems = _state.value.contactListItems,
                    dataRepository = dataRepository,
                )
                _state.update { it.copy(isStaging = false) }
                action.onStagingDone()
            }
        }
    }
}