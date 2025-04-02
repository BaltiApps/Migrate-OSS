package balti.migrate.backup.ui.screens.listScreen.callLogBackup

import balti.migrate.backup.ui.screens.listScreen.ListScreenGenericViewModel
import baltiapps.migrate.domain.backup.model.CallLogListItem
import baltiapps.migrate.domain.backup.model.Progress
import baltiapps.migrate.domain.backup.repository.DataRepository
import baltiapps.migrate.domain.backup.usecase.ReadCallLogUseCase
import baltiapps.migrate.domain.backup.usecase.StageSelectedCallLogs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class CallLogBackupViewModel(
    private val readCallLogUseCase: ReadCallLogUseCase,
    private val stageSelectedCallLogs: StageSelectedCallLogs,
    private val dataRepository: DataRepository,
): ListScreenGenericViewModel<CallLogListItem>() {

    private val _state = MutableStateFlow(CallLogBackupState())
    val state = _state.asStateFlow()

    override fun getListFromState(): List<CallLogListItem> {
        return _state.value.callLogList
    }

    override fun updateStateWithProgress(progress: Progress) {
        _state.update {
            it.copy(progress = progress)
        }
    }

    override fun updateStateStaging(isStaging: Boolean) {
        _state.update {
            it.copy(isStaging = isStaging)
        }
    }

    override fun updateStateWithItems(list: List<CallLogListItem>) {
        _state.update {
            it.copy(callLogList = list)
        }
    }

    init {
        super.readItems(
            reader = readCallLogUseCase::invoke,
            getReadItems = dataRepository::getReadListItems
        )
    }

    fun performAction(action: CallLogBackupAction) {
        when(action) {
            is CallLogBackupAction.ToggleCallLogItem -> {
                super.toggleItem(action.item)
            }
            is CallLogBackupAction.ToggleAllCallLog -> {
                super.toggleAll(action.isChecked)
            }
            is CallLogBackupAction.StageCallLogs -> {
                super.stageItems(
                    stagingBlock = stageSelectedCallLogs::invoke,
                    onStagingDone = { action.onStagingDone() }
                )
            }
        }
    }
}