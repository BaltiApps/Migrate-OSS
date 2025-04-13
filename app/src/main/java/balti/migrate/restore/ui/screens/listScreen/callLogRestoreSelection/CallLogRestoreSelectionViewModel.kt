package balti.migrate.restore.ui.screens.listScreen.callLogRestoreSelection

import android.Manifest
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import balti.migrate.MainActivity
import balti.migrate.common.utils.ListItemUtils
import baltiapps.migrate.domain.common.sources.ContextSource
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
    private val contextSource: ContextSource,
) : ViewModel() {

    private val _state = MutableStateFlow(CallLogRestoreSelectionState())
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
            readCallLogForRestoreUseCase.invoke().onCompletion {
                _state.update {
                    it.copy(
                        hasPermission = true,
                        callLogList = dataRepository.callLogListItems
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

    fun onAction(action: CallLogRestoreSelectionAction) = viewModelScope.launch {
        when(action) {
            is CallLogRestoreSelectionAction.RequestPermission -> {
                if (action.activity !is MainActivity) return@launch
                action.activity.requestPermissions(permissionList) {
                    if (it) loadData()
                }
            }
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