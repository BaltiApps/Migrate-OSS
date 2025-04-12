package balti.migrate.backup.ui.screens.listScreen.callLogBackup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import balti.migrate.common.utils.ListItemUtils
import baltiapps.migrate.domain.backup.repository.BackupDataRepository
import baltiapps.migrate.domain.backup.usecase.ReadCallLogForBackupUseCase
import baltiapps.migrate.domain.common.usecase.StageSelectedCallLogs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CallLogBackupViewModel(
    private val listItemUtils: ListItemUtils,
    private val readCallLogForBackupUseCase: ReadCallLogForBackupUseCase,
    private val stageSelectedCallLogs: StageSelectedCallLogs,
    private val backupDataRepository: BackupDataRepository,
): ViewModel() {

    private val _state = MutableStateFlow(CallLogBackupState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            readCallLogForBackupUseCase.invoke().onCompletion {
                _state.update {
                    it.copy(
                        callLogList = backupDataRepository.callLogListItems
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

    fun performAction(action: CallLogBackupAction) {
        when(action) {
            is CallLogBackupAction.ToggleCallLogItem -> {
                val result = listItemUtils.toggleSingleItem(_state.value.callLogList, action.item)
                _state.update { it.copy(callLogList = result) }
            }
            is CallLogBackupAction.ToggleAllCallLog -> {
                val result = listItemUtils.toggleAllItems(_state.value.callLogList, action.isChecked)
                _state.update { it.copy(callLogList = result) }
            }
            is CallLogBackupAction.StageCallLogs -> {
                _state.update { it.copy(isStaging = true) }
                stageSelectedCallLogs.invoke(
                    allListItems = _state.value.callLogList,
                    dataRepository = backupDataRepository,
                )
                _state.update { it.copy(isStaging = false) }
                action.onStagingDone()
            }
        }
    }
}