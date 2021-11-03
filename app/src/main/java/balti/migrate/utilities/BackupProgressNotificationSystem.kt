package balti.migrate.utilities

import android.os.Bundle
import balti.migrate.R
import balti.migrate.backupEngines.BackupServiceKotlin_new
import balti.migrate.extraBackupsActivity.ExtraBackupsKotlin
import balti.migrate.simpleActivities.ProgressShowActivity_new
import balti.migrate.utilities.CommonToolsKotlin.Companion.EXTRA_ERRORS
import balti.migrate.utilities.CommonToolsKotlin.Companion.EXTRA_FINISHED_ZIP_PATHS
import balti.migrate.utilities.CommonToolsKotlin.Companion.EXTRA_IS_CANCELLED
import balti.migrate.utilities.CommonToolsKotlin.Companion.EXTRA_TOTAL_TIME
import balti.migrate.utilities.CommonToolsKotlin.Companion.EXTRA_WARNINGS
import balti.module.baltitoolbox.functions.GetResources.getStringFromRes
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
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
             * Also created in [BackupServiceKotlin_new.finishBackup].
             */
            val extraInfoBundle: Bundle? = null
        )

        /**
         * If this value is set, all new updates in [emitMessage] will have their
         * - title changed to "Cancelling backup"
         * - type changed to [ProgressType.PROGRESS_TYPE_WAITING_TO_CANCEL].
         *
         * Also broadcast [cancellingUpdate] if set value is true.
         */
        var cancellingLock = false
        set(value) {
            if (value) emitMessage(cancellingUpdate)
            field = value
        }

        private val cancellingUpdate by lazy {
            BackupUpdate(
                ProgressType.PROGRESS_TYPE_WAITING_TO_CANCEL,
                getStringFromRes(R.string.cancelling),
                getStringFromRes(R.string.please_wait),
                "", 0
            )
        }

        private val initialValue = BackupUpdate(
            ProgressType.EMPTY,
            "", "", "", 0
        )

        /**
         * Using SharedFlow to store last 100 messages
         * for display purposes in [ProgressShowActivity_new].
         *
         * Stateflow is mainly used for cases in which past value from a previous backup could cause malfunction.
         * For example, [BackupServiceKotlin_new.progressWriter] does not need old values for the current backup.
         * Similarly [ExtraBackupsKotlin.listenForUpdatesToStartProgressActivity] will be triggered
         * for values of a previous backup if it doesn't listen to only current values.
         */
        private val mutableStateFlow by lazy { MutableStateFlow(initialValue) }
        private val mutableSharedFlow by lazy { MutableSharedFlow<BackupUpdate>(100) }
        private val stateFlow = mutableStateFlow.asStateFlow()
        private val sharedFlow = mutableSharedFlow.asSharedFlow()

        /**
         * Keeping suspend function to encourage listeners to listen from lifecycleScope.
         * @param getCache Send last 100 cached messages if true.
         * @param listenOnce If `true` then the function [f] will be only launched once
         * for the first received update, rest all updates will be ignored.
         */
        suspend fun addListener(getCache: Boolean, listenOnce: Boolean = false, f: (update: BackupUpdate) -> Unit){
            var dontLaunch = false
            val bufferCapacity = 1000
            if (getCache && !listenOnce) {
                sharedFlow.buffer(bufferCapacity).collect {
                    f(it)
                }
            }
            else {
                stateFlow.buffer(bufferCapacity).collect {
                    if (it != initialValue) {
                        if (!dontLaunch) f(it)
                        if (listenOnce) dontLaunch = true
                    }
                }
            }
        }

        /**
         * Send a message to all the listeners.
         * Block a lot of parts of the update if [cancellingLock] is set to `true`.
         * Always allow update if type is [ProgressType.BACKUP_CANCELLED].
         */
        fun emitMessage(update: BackupUpdate){
            GlobalScope.launch {
                if (update.type == ProgressType.BACKUP_CANCELLED || !cancellingLock) {
                    mutableStateFlow.emit(update)
                    mutableSharedFlow.emit(update)
                }
                else {
                    BackupUpdate(
                        cancellingUpdate.type,
                        cancellingUpdate.title,
                        cancellingUpdate.subTask,
                        update.log,
                        update.progressPercent,
                        update.extraInfoBundle
                    ).let {
                        mutableStateFlow.emit(it)
                        mutableSharedFlow.emit(it)
                    }
                }
            }
        }

        /**
         * Reset the flow.
         * To be called after finishing the backup, just before closing the backup service.
         * If this is not called, [ExtraBackupsKotlin] will immediately receive a non empty value
         * of the last backup and start the [ProgressShowActivity_new] class.
         */
        fun resetImmediateUpdate(){
            GlobalScope.launch {
                delay(1000)
                mutableStateFlow.value = initialValue
            }
        }
    }
}