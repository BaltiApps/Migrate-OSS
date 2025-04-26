package balti.migrate.backup.ui.screens.progressScreen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import baltiapps.migrate.domain.common.repository.ProgressLogRepository
import baltiapps.migrate.domain.common.sources.ContextSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class BackupProgressScreenViewModel(
    private val contextSource: ContextSource,
    private val progressLogRepository: ProgressLogRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(BackupProgressScreenState.Empty)
    val state = _state.asStateFlow()

    private var isLogsPaused: Boolean = false

    private fun startObserving() {
        viewModelScope.launch {
            progressLogRepository.setProgressObserver { p, e ->
                if (p.isEmpty()) return@setProgressObserver
                val latestProgress = p.last()
                val isFinished = latestProgress.isBackupFinished()
                val cancelling = if (isFinished) false else _state.value.isCancelling
                if (isLogsPaused && !latestProgress.isBackupFinished()) return@setProgressObserver
                val headingText = contextSource.getProgressTitle(latestProgress.progressType)
                _state.update {
                    it.copy(
                        progressList = p,
                        errorList = e,
                        headingText = headingText,
                        isBackupFinished = latestProgress.isBackupFinished(),
                        isCancelling = cancelling
                    )
                }
            }
        }
    }

    init {
        startObserving()
    }

    fun performAction(action: BackupProgressScreenAction) {
        when (action) {
            is BackupProgressScreenAction.ToggleErrorOnly -> {
                _state.update { it.copy(errorOnly = action.enabled) }
            }
            is BackupProgressScreenAction.CancelBackup -> {
                _state.update {
                    it.copy(isCancelling = true)
                }
            }
            is BackupProgressScreenAction.PauseProgressLogs -> { isLogsPaused = true }
            is BackupProgressScreenAction.ResumeProgressLogs -> {
                isLogsPaused = false
                progressLogRepository.dispatchLatestObservedProgress()
            }
        }
    }

}