package balti.migrate.restore.ui.screens.progressScreen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import baltiapps.migrate.domain.common.repository.ProgressLogRepository
import baltiapps.migrate.domain.common.sources.ContextSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class RestoreProgressScreenViewModel(
    private val contextSource: ContextSource,
    private val progressLogRepository: ProgressLogRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(RestoreProgressScreenState.Empty)
    val state = _state.asStateFlow()

    private var observeErrorsOnly: Boolean = false
    private var isCancelling: Boolean = false
    private var isLogsPaused: Boolean = false

    private fun startObserving() {
        viewModelScope.launch {
            progressLogRepository.setProgressObserver { p, e ->
                if (isLogsPaused) return@setProgressObserver
                val progressList = if (observeErrorsOnly) e else p
                if (p.isEmpty()) return@setProgressObserver
                val latestProgress = p.last()
                val headingText = contextSource.getProgressTitle(latestProgress.progressType)
                _state.update {
                    it.copy(
                        progressList = progressList,
                        headingText = headingText,
                        isRestoreFinished = latestProgress.isRestoreFinished(),
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

    fun performAction(action: RestoreProgressScreenAction) {
        when (action) {
            is RestoreProgressScreenAction.ToggleErrorOnly -> {
                observeErrorsOnly = action.enabled
                progressLogRepository.dispatchLatestObservedProgress()
            }
            is RestoreProgressScreenAction.CancelRestore -> {
                _state.update {
                    isCancelling = true
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