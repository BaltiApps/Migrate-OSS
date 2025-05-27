package balti.migrate.backup.ui.screens.listScreen.contactBackupSelection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import balti.migrate.common.utils.ListItemUtils
import balti.migrate.common.utils.PermissionUtils
import baltiapps.migrate.domain.backup.repository.BackupDataRepository
import baltiapps.migrate.domain.backup.usecase.ReadContactsForBackupUseCase
import baltiapps.migrate.domain.common.sources.ContextSource
import baltiapps.migrate.domain.common.usecase.StageSelectedContacts
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ContactBackupSelectionViewModel(
    private val listItemUtils: ListItemUtils,
    private val readContactsForBackupUseCase: ReadContactsForBackupUseCase,
    private val stageSelectedContacts: StageSelectedContacts,
    private val backupDataRepository: BackupDataRepository,
    private val contextSource: ContextSource,
) : ViewModel() {

    private val _state = MutableStateFlow(ContactBackupSelectionState())
    val state = _state.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        if (!contextSource.checkPermission(PermissionUtils.contactsReadPermission)) {
            _state.update { it.copy(hasPermission = false) }
            return
        }
        viewModelScope.launch {
            readContactsForBackupUseCase.invoke().onCompletion {
                _state.update {
                    it.copy(
                        progress = it.progress.copy(percentage = 1.0),
                        hasPermission = true,
                        contactList = backupDataRepository.contactsListItems
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

    fun performAction(action: ContactBackupSelectionAction) = viewModelScope.launch {
        when (action) {
            is ContactBackupSelectionAction.OnPermissionResult -> {
                if (action.isGranted) loadData()
            }
            is ContactBackupSelectionAction.ToggleContactItem -> {
                val result = listItemUtils.toggleSingleItem(_state.value.contactList, action.item)
                _state.update { it.copy(contactList = result) }
            }
            is ContactBackupSelectionAction.ToggleAllContacts -> {
                val result = listItemUtils.toggleAllItems(
                    list = _state.value.contactList,
                    isChecked = action.isChecked,
                    filter = {
                        if (_state.value.syncedContactsExpanded) {
                            !it.isLocalContact
                        } else if (_state.value.localContactsExpanded) {
                            it.isLocalContact
                        } else false
                    }
                )
                _state.update { it.copy(contactList = result) }
            }
            is ContactBackupSelectionAction.StageContacts -> {
                _state.update { it.copy(isStaging = true) }
                stageSelectedContacts.invoke(
                    allListItems = _state.value.contactList,
                    dataRepository = backupDataRepository,
                )
                _state.update { it.copy(isStaging = false) }
                action.onStagingDone()
            }
            is ContactBackupSelectionAction.ToggleSyncedContactsVisibility -> {
                _state.update { it.copy(syncedContactsExpanded = action.isVisible) }
            }
            is ContactBackupSelectionAction.ToggleLocalContactsVisibility -> {
                _state.update { it.copy(localContactsExpanded = action.isVisible) }
            }
        }
    }
}