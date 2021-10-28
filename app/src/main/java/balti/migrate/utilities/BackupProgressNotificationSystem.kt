package balti.migrate.utilities

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class BackupProgressNotificationSystem {
    companion object {

        enum class ProgressType {
            BACKUP_CANCELLED,
            PROGRESS_TYPE_TESTING,
            PROGRESS_TYPE_CONTACTS,
            PROGRESS_TYPE_SMS,

        }

        data class BackupUpdate(
            val type: ProgressType,
            val title: String,
            val subTask: String,
            val log: String,
            val progressPercent: Int,
        )

        /**
         * Using SharedFlow to store last 100 messages
         * for display purposes in [balti.migrate.simpleActivities.ProgressShowActivity].
         */
        private val mutableFlow by lazy { MutableSharedFlow<BackupUpdate>(100) }
        private val flow = mutableFlow.asSharedFlow()

        /**
         * Keeping suspend function to encourage listeners to listen from lifecycleScope.
         * @param getCache Send last 100 cached messages if true.
         */
        suspend fun addListener(getCache: Boolean, f: (update: BackupUpdate) -> Unit){
            if (getCache) {
                flow.replayCache.forEach {
                    f(it)
                }
            }
            flow.buffer(1000).collect {
                f(it)
            }
        }

        /**
         * Send a message to all the listeners.
         */
        fun emitMessage(update: BackupUpdate){
            GlobalScope.launch {
                mutableFlow.emit(update)
            }
        }
    }
}