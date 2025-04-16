package balti.migrate.restore.ui.screens.listScreen.smsRestoreSelection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import balti.migrate.common.utils.ListItemUtils
import baltiapps.migrate.domain.common.usecase.StageSelectedSms
import baltiapps.migrate.domain.restore.repository.RestoreDataRepository
import baltiapps.migrate.domain.restore.usecase.ReadSmsForRestoreUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SmsRestoreSelectionViewModel(
    private val listItemUtils: ListItemUtils,
    private val dataRepository: RestoreDataRepository,
    private val readSmsForRestoreUseCase: ReadSmsForRestoreUseCase,
    private val stageSelectedSms: StageSelectedSms,
): ViewModel() {

    private val _state = MutableStateFlow(SmsRestoreSelectionState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            readSmsForRestoreUseCase.invoke().onCompletion {
                _state.update {
                    it.copy(
                        progress = it.progress.copy(percentage = 1.0),
                        smsList = dataRepository.smsListItems
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

    fun onAction(action: SmsRestoreSelectionAction) = viewModelScope.launch {
        when(action) {
            is SmsRestoreSelectionAction.ToggleSmsItem -> {
                val result = listItemUtils.toggleSingleItem(_state.value.smsList, action.item)
                _state.update { it.copy(smsList = result) }
            }
            is SmsRestoreSelectionAction.ToggleAllSms -> {
                val result = listItemUtils.toggleAllItems(_state.value.smsList, action.isChecked)
                _state.update { it.copy(smsList = result) }
            }
            is SmsRestoreSelectionAction.StageSms -> {
                _state.update { it.copy(isStaging = true) }
                stageSelectedSms.invoke(
                    allListItems = _state.value.smsList,
                    dataRepository = dataRepository,
                )
                _state.update { it.copy(isStaging = false) }
                action.onStagingDone()
            }
        }
    }
}