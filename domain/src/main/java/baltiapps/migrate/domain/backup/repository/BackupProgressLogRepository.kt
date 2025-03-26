package baltiapps.migrate.domain.backup.repository

import baltiapps.migrate.domain.backup.model.Progress
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

abstract class BackupProgressLogRepository {
    abstract val truncatedIndicator: Progress

    private val progressLogQueue = ArrayDeque<Progress>()
    private val errorLogQueue = ArrayDeque<Progress>()

    private val progressFlow = MutableStateFlow(listOf<Progress>())
    private val errorFlow = MutableStateFlow(listOf<Progress>())

    private var observer: ((List<Progress>, List<Progress>) -> Unit)? = null

    private val mutex = Mutex()

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

    fun dispatchLatestObservedProgress() {
        observer?.invoke(progressFlow.value, errorFlow.value)
    }

    fun getLatestProgress(): Progress {
        return progressFlow.value.lastOrNull() ?: Progress.Empty
    }

    suspend fun reset() {
        progressLogQueue.clear()
        errorLogQueue.clear()
        progressFlow.emit(listOf())
        errorFlow.emit(listOf())
    }
}