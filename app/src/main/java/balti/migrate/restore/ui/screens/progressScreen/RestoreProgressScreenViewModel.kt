package balti.migrate.restore.ui.screens.progressScreen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import balti.migrate.restore.data.service.RestoreService
import baltiapps.migrate.domain.common.repository.ProgressLogRepository
import baltiapps.migrate.domain.common.sources.ContextSource
import baltiapps.migrate.domain.common.sources.Preferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class RestoreProgressScreenViewModel(
    private val contextSource: ContextSource,
    private val preferences: Preferences,
    private val progressLogRepository: ProgressLogRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(RestoreProgressScreenState.Empty)
    val state = _state.asStateFlow()

    private var isLogsPaused: Boolean = false

    private fun startObserving() {
        viewModelScope.launch {
            progressLogRepository.setProgressObserver { p, e ->
                if (p.isEmpty()) return@setProgressObserver
                val latestProgress = p.last()
                val isFinished = latestProgress.isRestoreFinished()
                val isCancelling = if (isFinished) false else _state.value.isCancelling
                if (isLogsPaused && !isFinished) return@setProgressObserver
                val headingText = contextSource.getProgressTitle(latestProgress.progressType)
                _state.update {
                    it.copy(
                        progressList = p,
                        errorList = e,
                        headingText = headingText,
                        isRestoreFinished = latestProgress.isRestoreFinished(),
                        isCancelling = isCancelling,
                    )
                }
            }
        }
    }

    init {
        startObserving()
        if (!RestoreService.isRunning) {
            viewModelScope.launch(Dispatchers.IO) {
                progressLogRepository.populateWithValues(
                    progressList = preferences.getLastSavedRestoreProgressList(),
                    errorList = preferences.getLastSavedRestoreErrorList(),
                )
            }
        }
    }

    fun performAction(action: RestoreProgressScreenAction) {
        when (action) {
            is RestoreProgressScreenAction.ToggleErrorOnly -> {
                _state.update { it.copy(isCancelling = action.enabled) }
            }
            is RestoreProgressScreenAction.CancelRestore -> {
                _state.update {
                    it.copy(isCancelling = true)
                }
            }
            is RestoreProgressScreenAction.PauseProgressLogs -> { isLogsPaused = true }
            is RestoreProgressScreenAction.ResumeProgressLogs -> {
                isLogsPaused = false
                progressLogRepository.dispatchLatestObservedProgress()
            }
        }
    }

}