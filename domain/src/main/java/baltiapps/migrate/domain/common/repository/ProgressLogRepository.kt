package baltiapps.migrate.domain.common.repository

import baltiapps.migrate.domain.common.model.Progress
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

abstract class ProgressLogRepository {
    abstract val truncatedIndicator: Progress

    private val progressLogQueue = ArrayDeque<Progress>()
    private val errorLogQueue = ArrayDeque<Progress>()

    private val progressFlow = MutableStateFlow(listOf<Progress>())
    private val errorFlow = MutableStateFlow(listOf<Progress>())

    private var observer: ((List<Progress>, List<Progress>) -> Unit)? = null

    private val mutex = Mutex()

    var isAnyErrorPresent = false
    private set

    companion object {
        private const val LOG_CACHE = 1000
    }

    private suspend fun push(
        progress: Progress,
        queue: ArrayDeque<Progress>,
        flow: MutableStateFlow<List<Progress>>,
        truncatedIndicator: Progress,
    ) {
        mutex.withLock {
            queue.addLast(progress)
            while (queue.size > LOG_CACHE) {
                queue.removeFirst()
            }
            if (queue.size == LOG_CACHE) {
                queue.addFirst(truncatedIndicator)
            }
            flow.value = queue.toList()
        }
    }

    suspend fun pushProgress(progress: Progress) {
        push(
            progress = progress,
            queue = progressLogQueue,
            flow = progressFlow,
            truncatedIndicator = truncatedIndicator
        )
    }

    suspend fun pushError(progress: Progress) {
        isAnyErrorPresent =
            isAnyErrorPresent || progress.isFailure     // don't be false if once turned true
        push(
            progress = progress,
            queue = errorLogQueue,
            flow = errorFlow,
            truncatedIndicator = truncatedIndicator
        )
    }

    suspend fun setProgressObserver(
        observer: (List<Progress>, List<Progress>) -> Unit,
    ) {
        this.observer = observer
        combine(progressFlow, errorFlow) { p, e ->
            p to e
        }.collect { (p, e) ->
            observer(p, e)
        }
    }

    fun populateWithValues(
        progressList: List<Progress>,
        errorList: List<Progress>,
    ) {
        progressLogQueue.clear()
        progressLogQueue.addAll(progressList)
        progressFlow.value = progressLogQueue.toList()
        errorLogQueue.clear()
        errorLogQueue.addAll(errorList)
        errorFlow.value = errorLogQueue.toList()
    }

    fun dispatchLatestObservedProgress() {
        observer?.invoke(progressFlow.value, errorFlow.value)
    }

    fun getLatestProgress(): Progress {
        return progressFlow.value.lastOrNull() ?: Progress.Empty
    }

    suspend fun reset() {
        isAnyErrorPresent = false
        progressLogQueue.clear()
        errorLogQueue.clear()
        progressFlow.emit(listOf())
        errorFlow.emit(listOf())
    }

    fun getDisplayedProgressList(): List<Progress> {
        return progressLogQueue
    }

    fun getDisplayedErrorList(): List<Progress> {
        return errorLogQueue
    }
}