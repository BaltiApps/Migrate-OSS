package balti.migrate.backup.ui.screens.listScreen.callLogBackupSelection

import android.Manifest
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import balti.migrate.MainActivity
import balti.migrate.common.utils.ListItemUtils
import baltiapps.migrate.domain.backup.repository.BackupDataRepository
import baltiapps.migrate.domain.backup.usecase.ReadCallLogForBackupUseCase
import baltiapps.migrate.domain.common.sources.ContextSource
import baltiapps.migrate.domain.common.usecase.StageSelectedCallLogs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CallLogBackupSelectionViewModel(
    private val listItemUtils: ListItemUtils,
    private val readCallLogForBackupUseCase: ReadCallLogForBackupUseCase,
    private val stageSelectedCallLogs: StageSelectedCallLogs,
    private val backupDataRepository: BackupDataRepository,
    private val contextSource: ContextSource,
): ViewModel() {

    private val _state = MutableStateFlow(CallLogBackupSelectionState())
    val state = _state.asStateFlow()

    private val permissionList = listOf(
        Manifest.permission.READ_CALL_LOG,
        Manifest.permission.WRITE_CALL_LOG,
    )

    init {
        loadData()
    }

    private fun loadData() {
        if (!contextSource.checkPermissions(permissionList)) {
            _state.update { it.copy(hasPermission = false) }
            return
        }
        viewModelScope.launch {
            readCallLogForBackupUseCase.invoke().onCompletion {
                _state.update {
                    it.copy(
                        hasPermission = true,
                        callLogList = backupDataRepository.callLogListItems
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

    fun performAction(action: CallLogBackupSelectionAction) = viewModelScope.launch {
        when(action) {
            is CallLogBackupSelectionAction.RequestPermission -> {
                if (action.activity !is MainActivity) return@launch
                action.activity.requestPermissions(permissionList) {
                    if (it) loadData()
                }
            }
            is CallLogBackupSelectionAction.ToggleCallLogItem -> {
                val result = listItemUtils.toggleSingleItem(_state.value.callLogList, action.item)
                _state.update { it.copy(callLogList = result) }
            }
            is CallLogBackupSelectionAction.ToggleAllCallLog -> {
                val result = listItemUtils.toggleAllItems(_state.value.callLogList, action.isChecked)
                _state.update { it.copy(callLogList = result) }
            }
            is CallLogBackupSelectionAction.StageCallLogs -> {
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