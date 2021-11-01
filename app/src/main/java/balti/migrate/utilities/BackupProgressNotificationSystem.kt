package balti.migrate.utilities

import android.os.Bundle
import balti.migrate.backupEngines.BackupServiceKotlin_new
import balti.migrate.simpleActivities.ProgressShowActivity_new
import balti.migrate.utilities.CommonToolsKotlin.Companion.EXTRA_ERRORS
import balti.migrate.utilities.CommonToolsKotlin.Companion.EXTRA_FINISHED_ZIP_PATHS
import balti.migrate.utilities.CommonToolsKotlin.Companion.EXTRA_IS_CANCELLED
import balti.migrate.utilities.CommonToolsKotlin.Companion.EXTRA_TOTAL_TIME
import balti.migrate.utilities.CommonToolsKotlin.Companion.EXTRA_WARNINGS
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
             * - [EXTRA_ERRORS] : ArrayList of errors (Strings).
             * - [EXTRA_WARNINGS] : ArrayList of warnings (Strings).
             * - [EXTRA_IS_CANCELLED] : Boolean - `true` if cancelled.
             * - [EXTRA_TOTAL_TIME] : Long - time in milli seconds.
             * - [EXTRA_FINISHED_ZIP_PATHS] : ArrayList of zip file paths.
             *
             * Read in [ProgressShowActivity_new.updateUiOnBackupFinished].
             * Also created in [ProgressShowActivity_new.createFinishedUpdateFromIntent].
             * Also created in [BackupServiceKotlin_new.finishBackup].
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