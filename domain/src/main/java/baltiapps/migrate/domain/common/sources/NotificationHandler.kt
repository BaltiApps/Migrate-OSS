package baltiapps.migrate.domain.common.sources

import baltiapps.migrate.domain.common.model.Progress
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

abstract class NotificationHandler<T> {

    companion object {
        private const val SAFE_NOTIFICATION_INTERVAL = 300L
    }

    protected lateinit var progressSamplerJob: Job

    abstract fun setup()
    abstract fun getInitialNotification(): T
    abstract fun getFinishedNotification(subtitle: String? = null): T
    abstract fun getCancelledNotification(subtitle: String? = null): T
    abstract fun getFinishedWithErrorNotification(subtitle: String? = null): T
    abstract fun getProgressNotification(progress: Progress): T
    abstract fun displayNotification(notification: T)
    protected abstract suspend fun getLatestProgress(): Progress

    fun listenAtSafeIntervals() {
        CoroutineScope(progressSamplerJob).launch {
            while (true) {
                ensureActive()
                val latestProgress = getLatestProgress()
                if (latestProgress.isLogHeading) continue
                withContext(Dispatchers.Main) {
                    displayNotification(getProgressNotification(latestProgress))
                }
                delay(SAFE_NOTIFICATION_INTERVAL)
            }
        }
    }

    fun stopListening() {
        progressSamplerJob.cancel()
    }
}