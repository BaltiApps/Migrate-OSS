package balti.migrate.backup.ui.screens.progressScreen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import baltiapps.migrate.domain.backup.repository.BackupProgressLogRepository
import baltiapps.migrate.domain.backup.sources.ContextSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ProgressScreenViewModel(
    private val contextSource: ContextSource,
    private val backupProgressLogRepository: BackupProgressLogRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ProgressScreenState.Empty)
    val state = _state.asStateFlow()

    private var observeErrorsOnly: Boolean = false
    private var isCancelling: Boolean = false
    private var isLogsPaused: Boolean = false

    private fun startObserving() {
        viewModelScope.launch {
            backupProgressLogRepository.setProgressObserver {p, e ->
                if (isLogsPaused) return@setProgressObserver
                val progressList = if (observeErrorsOnly) e else p
                if (progressList.isEmpty()) return@setProgressObserver
                val latestProgress = progressList.last()
                val headingText = contextSource.getProgressTitle(latestProgress.progressType)
                _state.update {
                    it.copy(
                        progressList = progressList,
                        headingText = headingText,
                        isBackupFinished = latestProgress.isFinished(),
                        isCancelling = isCancelling,
                        errorOnly = observeErrorsOnly,
                    )
                }
            }
        }
    }

    init {
        startObserving()
    }

    fun performAction(action: ProgressScreenAction) {
        when (action) {
            is ProgressScreenAction.ToggleErrorOnly -> {
                observeErrorsOnly = action.enabled
                backupProgressLogRepository.dispatchLatestObservedProgress()
            }
            is ProgressScreenAction.CancelBackup -> {
                _state.update {
                    isCancelling = true
                    it.copy(isCancelling = true)
                }
            }
            is ProgressScreenAction.PauseProgressLogs -> { isLogsPaused = true }
            is ProgressScreenAction.ResumeProgressLogs -> {
                isLogsPaused = false
                backupProgressLogRepository.dispatchLatestObservedProgress()
            }
        }
    }

}