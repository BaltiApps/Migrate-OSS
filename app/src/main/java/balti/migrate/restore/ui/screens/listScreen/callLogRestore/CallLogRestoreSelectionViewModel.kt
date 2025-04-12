package balti.migrate.restore.ui.screens.listScreen.callLogRestore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import balti.migrate.common.utils.ListItemUtils
import baltiapps.migrate.domain.common.usecase.StageSelectedCallLogs
import baltiapps.migrate.domain.restore.repository.RestoreDataRepository
import baltiapps.migrate.domain.restore.usecase.ReadCallLogForRestoreUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CallLogRestoreSelectionViewModel(
    private val listItemUtils: ListItemUtils,
    private val dataRepository: RestoreDataRepository,
    private val readCallLogForRestoreUseCase: ReadCallLogForRestoreUseCase,
    private val stageSelectedCallLogs: StageSelectedCallLogs,
) : ViewModel() {

    private val _state = MutableStateFlow(CallLogRestoreSelectionState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            readCallLogForRestoreUseCase.invoke().onCompletion {
                _state.update {
                    it.copy(
                        callLogList = dataRepository.callLogListItems
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

    fun onAction(action: CallLogRestoreSelectionAction) = viewModelScope.launch {
        when(action) {
            is CallLogRestoreSelectionAction.ToggleCallLogItem -> {
                val result = listItemUtils.toggleSingleItem(_state.value.callLogList, action.item)
                _state.update { it.copy(callLogList = result) }
            }
            is CallLogRestoreSelectionAction.ToggleAllCallLog -> {
                val result = listItemUtils.toggleAllItems(_state.value.callLogList, action.isChecked)
                _state.update { it.copy(callLogList = result) }
            }
            is CallLogRestoreSelectionAction.StageCallLogs -> {
                _state.update { it.copy(isStaging = true) }
                stageSelectedCallLogs.invoke(
                    allListItems = _state.value.callLogList,
                    dataRepository = dataRepository,
                )
                _state.update { it.copy(isStaging = false) }
                action.onStagingDone()
            }
        }
    }
}