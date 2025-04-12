package balti.migrate.backup.ui.screens.listScreen.smsBackupSelection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import balti.migrate.common.utils.ListItemUtils
import baltiapps.migrate.domain.backup.repository.BackupDataRepository
import baltiapps.migrate.domain.backup.usecase.ReadSmsForBackupUseCase
import baltiapps.migrate.domain.common.usecase.StageSelectedSms
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SmsBackupSelectionViewModel(
    private val listItemUtils: ListItemUtils,
    private val readSmsForBackupUseCase: ReadSmsForBackupUseCase,
    private val stageSelectedSms: StageSelectedSms,
    private val backupDataRepository: BackupDataRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(SmsBackupSelectionState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            readSmsForBackupUseCase.invoke().onCompletion {
                _state.update {
                    it.copy(smsList = backupDataRepository.smsListItems)
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

    fun performAction(action: SmsBackupSelectionAction) = viewModelScope.launch {
        when(action) {
            is SmsBackupSelectionAction.ToggleSmsItem -> {
                val result = listItemUtils.toggleSingleItem(_state.value.smsList, action.item)
                _state.update { it.copy(smsList = result) }
            }
            is SmsBackupSelectionAction.ToggleAllSms -> {
                val result = listItemUtils.toggleAllItems(_state.value.smsList, action.isChecked)
                _state.update { it.copy(smsList = result) }
            }
            is SmsBackupSelectionAction.StageSms -> {
                _state.update { it.copy(isStaging = true) }
                stageSelectedSms.invoke(
                    allListItems = _state.value.smsList,
                    dataRepository = backupDataRepository,
                )
                _state.update { it.copy(isStaging = false) }
                action.onStagingDone()
            }
        }
    }
}