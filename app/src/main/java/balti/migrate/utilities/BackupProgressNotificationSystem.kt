package balti.migrate.utilities

import android.os.Bundle
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
            PROGRESS_TYPE_FINISHED,
            PROGRESS_TYPE_WAITING_TO_CANCEL,

        }

        data class BackupUpdate(
            val type: ProgressType,
            val title: String,
            val subTask: String,
            val log: String,
            val progressPercent: Int,

            /**
             * Bundle to contain information like:
             * - ArrayList of errors.
             * - ArrayList of warnings.
             * - ArrayList of zip file paths.
             */
            val extraInfoBundle: Bundle? = null
        )

        /**
         * Using SharedFlow to store last 100 messages
         * for display purposes in [balti.migrate.simpleActivities.ProgressShowActivity_new].
         */
        private val mutableFlow by lazy { MutableSharedFlow<BackupUpdate>(100) }
        private val flow = mutableFlow.asSharedFlow()

        /**
         * Keeping suspend function to encourage listeners to listen from lifecycleScope.
         * @param getCache Send last 100 cached messages if true.
         * @param listenOnce If `true` then the function [f] will be only launched once
         * for the first received update, rest all updates will be ignored.
         */
        suspend fun addListener(getCache: Boolean, listenOnce: Boolean = false, f: (update: BackupUpdate) -> Unit){
            var dontLaunch = false
            if (getCache && !listenOnce) {
                flow.replayCache.forEach {
                    f(it)
                }
            }
            flow.buffer(1000).collect {
                if (!dontLaunch) f(it)
                if (listenOnce) dontLaunch = true
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