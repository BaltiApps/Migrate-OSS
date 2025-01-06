package balti.migrate.backup.ui.screens.progressScreen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import balti.migrate.backup.data.service.BackupService
import baltiapps.migrate.domain.backup.model.Progress
import baltiapps.migrate.domain.backup.sources.PlatformContextSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ProgressScreenViewModel(
    private val contextSource: PlatformContextSource,
) : ViewModel() {

    private val _state = MutableStateFlow(ProgressScreenState.Empty)
    val state = _state.asStateFlow()

    private var observeErrorsOnly: Boolean = false
    private var isCancelling: Boolean = false
    private var isLogsPaused: Boolean = false

    private fun startObserving() {
        viewModelScope.launch {
            BackupService.backupProgress.collect { progress ->
                if (!isLogsPaused) {
                    publishProgress(progress)
                }
            }
        }
    }

    init {
        startObserving()
    }

    private fun getTruncatedLogs(logs: List<Progress>): List<Progress> {
        val lastLogs = logs.takeLast(BackupService.LOG_CACHE)
        return if (logs.size == BackupService.LOG_CACHE) {
            listOf(BackupService.truncatedProgress) + lastLogs
        } else lastLogs
    }

    private fun publishProgress(collectedProgress: Progress?) {

        val latestProgress = collectedProgress ?: BackupService.progressCache.first()
        val isFinishedProgress = latestProgress.isFinished()

        val progressToShow =
            if (observeErrorsOnly) BackupService.errorsCache
            else BackupService.progressCache

        if (isFinishedProgress) {
            isCancelling = false
        }

        _state.update {
            it.copy(
                progressList = getTruncatedLogs(progressToShow),
                headingText = contextSource.getProgressTitle(latestProgress.progressType),
                isBackupFinished = isFinishedProgress,
                isCancelling = isCancelling,
                errorOnly = observeErrorsOnly,
            )
        }
    }

    fun performAction(action: ProgressScreenAction) {
        when (action) {
            is ProgressScreenAction.ToggleErrorOnly -> {
                observeErrorsOnly = action.enabled
                publishProgress(null)
            }
            is ProgressScreenAction.CancelBackup -> {
                _state.update {
                    isCancelling = true
                    it.copy(isCancelling = true)
                }
            }
            is ProgressScreenAction.PauseProgressLogs -> { isLogsPaused = true }
            is ProgressScreenAction.ResumeProgressLogs -> { isLogsPaused = false }
        }
    }

}