package baltiapps.migrate.domain.backup.notification

import baltiapps.migrate.domain.backup.model.Progress
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

abstract class PlatformNotificationHandler<T> {

    companion object {
        private const val SAFE_NOTIFICATION_INTERVAL = 300L
    }

    protected lateinit var progressSamplerJob: Job
    protected var currentNotification: T? = null

    abstract fun setup()
    abstract fun displayNotification(progress: Progress)
    abstract fun getInitialNotification(): T
    protected abstract suspend fun getLatestProgress(): Progress

    fun listenAtSafeIntervals() {
        CoroutineScope(progressSamplerJob).launch {
            while (true) {
                val latestProgress = getLatestProgress()
                if (latestProgress.isLogHeading) continue
                withContext(Dispatchers.Main) {
                    displayNotification(latestProgress)
                }
                delay(SAFE_NOTIFICATION_INTERVAL)
            }
        }
    }

    fun stopListening() {
        progressSamplerJob.cancel()
    }
}