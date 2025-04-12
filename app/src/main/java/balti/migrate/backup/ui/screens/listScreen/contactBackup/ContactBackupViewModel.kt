package balti.migrate.backup.ui.screens.listScreen.contactBackup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import balti.migrate.common.utils.ListItemUtils
import baltiapps.migrate.domain.backup.repository.BackupDataRepository
import baltiapps.migrate.domain.backup.usecase.ReadContactsForBackupUseCase
import baltiapps.migrate.domain.common.usecase.StageSelectedContacts
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ContactBackupViewModel(
    private val listItemUtils: ListItemUtils,
    private val readContactsForBackupUseCase: ReadContactsForBackupUseCase,
    private val stageSelectedContacts: StageSelectedContacts,
    private val backupDataRepository: BackupDataRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ContactBackupState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            readContactsForBackupUseCase.invoke().onCompletion {
                _state.update {
                    it.copy(
                        contactList = backupDataRepository.contactsListItems
                    )
                }
            }.collect {
                _state.update { state ->
                    state.copy(
                        progress = it
                    )
                }
            }
        }
    }

    fun performAction(action: ContactBackupAction) = viewModelScope.launch {
        when (action) {
            is ContactBackupAction.ToggleContactItem -> {
                val result = listItemUtils.toggleSingleItem(_state.value.contactList, action.item)
                _state.update { it.copy(contactList = result) }
            }
            is ContactBackupAction.ToggleAllContacts -> {
                val result = listItemUtils.toggleAllItems(_state.value.contactList, action.isChecked)
                _state.update { it.copy(contactList = result) }
            }
            is ContactBackupAction.StageContacts -> {
                _state.update { it.copy(isStaging = true) }
                stageSelectedContacts.invoke(
                    allListItems = _state.value.contactList,
                    dataRepository = backupDataRepository,
                )
                _state.update { it.copy(isStaging = false) }
                action.onStagingDone()
            }
        }
    }
}