package balti.migrate.backup.ui.screens.listScreen.smsBackupSelection

import android.Manifest
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import balti.migrate.MainActivity
import balti.migrate.common.utils.ListItemUtils
import baltiapps.migrate.domain.backup.repository.BackupDataRepository
import baltiapps.migrate.domain.backup.usecase.ReadSmsForBackupUseCase
import baltiapps.migrate.domain.common.sources.ContextSource
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
    private val contextSource: ContextSource,
) : ViewModel() {

    private val _state = MutableStateFlow(SmsBackupSelectionState())
    val state = _state.asStateFlow()

    private val permission = Manifest.permission.READ_SMS

    init {
        loadData()
    }

    private fun loadData() {
        if (!contextSource.checkPermission(permission)) {
            _state.update { it.copy(hasPermission = false) }
            return
        }
        viewModelScope.launch {
            readSmsForBackupUseCase.invoke().onCompletion {
                _state.update {
                    it.copy(
                        progress = it.progress.copy(percentage = 1.0),
                        hasPermission = true,
                        smsList = backupDataRepository.smsListItems,
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

    fun performAction(action: SmsBackupSelectionAction) = viewModelScope.launch {
        when(action) {
            is SmsBackupSelectionAction.RequestPermission -> {
                if (action.activity !is MainActivity) return@launch
                action.activity.requestPermission(permission) {
                    if (it) loadData()
                }
            }
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