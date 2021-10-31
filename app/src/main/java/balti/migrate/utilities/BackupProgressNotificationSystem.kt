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
            EMPTY,
            BACKUP_CANCELLED,
            PROGRESS_TYPE_TESTING,
            PROGRESS_TYPE_CONTACTS,
            PROGRESS_TYPE_SMS,
            PROGRESS_TYPE_CALLS,
            PROGRESS_TYPE_SETTINGS,
            PROGRESS_TYPE_MAKING_APP_SCRIPTS,
            PROGRESS_TYPE_APP_PROGRESS,
            PROGRESS_TYPE_MOVING_APP_FILES,
            PROGRESS_TYPE_VERIFYING,
            PROGRESS_TYPE_CORRECTING,
            PROGRESS_TYPE_MAKING_ZIP_BATCH,
            PROGRESS_TYPE_UPDATER_SCRIPT,
            PROGRESS_TYPE_ZIPPING,
            PROGRESS_TYPE_ZIP_VERIFICATION,

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