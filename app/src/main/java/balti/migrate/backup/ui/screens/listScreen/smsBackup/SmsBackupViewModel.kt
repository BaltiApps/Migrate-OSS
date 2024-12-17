package balti.migrate.backup.ui.screens.listScreen.smsBackup

import balti.migrate.backup.ui.screens.listScreen.ListSubScreenViewModel
import baltiapps.migrate.domain.backup.model.Progress
import baltiapps.migrate.domain.backup.model.SmsListItem
import baltiapps.migrate.domain.backup.usecase.ReadSmsUseCase
import baltiapps.migrate.domain.backup.usecase.StageSelectedSms
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class SmsBackupViewModel(
    private val readSmsUseCase: ReadSmsUseCase,
    private val stageSelectedSms: StageSelectedSms,
) : ListSubScreenViewModel<SmsListItem>() {

    private val _state = MutableStateFlow(SmsBackupState())
    val state = _state.asStateFlow()

    override fun getListFromState(): List<SmsListItem> {
        return _state.value.smsList
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

    override fun updateStateWithItems(list: List<SmsListItem>) {
        _state.update {
            it.copy(smsList = list)
        }
    }

    init {
        super.readItems(
            reader = readSmsUseCase::read,
            getReadItems = readSmsUseCase::getReadSms
        )
    }

    fun performAction(action: SmsBackupAction) {
        when(action) {
            is SmsBackupAction.ToggleSmsItem -> {
                super.toggleItem(action.item)
            }
            is SmsBackupAction.ToggleAllSms -> {
                super.toggleAll(action.isChecked)
            }
            is SmsBackupAction.StageSms -> {
                super.stageItems(
                    stagingBlock = stageSelectedSms::invoke,
                    onStagingDone = { action.onStagingDone() }
                )
            }
        }
    }
}