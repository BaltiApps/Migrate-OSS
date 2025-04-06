package balti.migrate.backup.data.service

import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import balti.migrate.common.data.sources.NotificationHandlerImpl.Companion.NOTIFICATION_ID_BACKUP_ONGOING
import balti.migrate.backup.di.Names
import balti.migrate.common.data.repository.ProgressLogRepositoryImpl.Companion.BREAK_LINE
import balti.migrate.common.data.sources.fileSystem.TextWriterImpl
import baltiapps.migrate.domain.ACTION_CANCEL_BACKUP
import baltiapps.migrate.domain.ACTION_START_BACKUP
import baltiapps.migrate.domain.BACKUP_ERROR_LOG
import baltiapps.migrate.domain.BACKUP_LOG
import baltiapps.migrate.domain.EXTRA_BACKUP_ROOT
import baltiapps.migrate.domain.common.model.DataItem
import baltiapps.migrate.domain.common.model.ListItem
import baltiapps.migrate.domain.common.model.Progress
import baltiapps.migrate.domain.common.sources.NotificationHandler
import baltiapps.migrate.domain.common.repository.ProgressLogRepository
import baltiapps.migrate.domain.backup.repository.BackupDataRepository
import baltiapps.migrate.domain.common.sources.ContextSource
import baltiapps.migrate.domain.common.sources.fileSystem.TextWriter
import baltiapps.migrate.domain.backup.usecase.BackupCallLogUseCase
import baltiapps.migrate.domain.backup.usecase.BackupContactsUseCase
import baltiapps.migrate.domain.backup.usecase.BackupSmsUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.core.qualifier.named
import timber.log.Timber
import java.io.File

class BackupService : LifecycleService() {

    private val contextSource: ContextSource by inject()
    private val repository: BackupDataRepository by inject()
    private val progressLogRepository: ProgressLogRepository by inject(named(Names.PROGRESS_LOG_REPOSITORY_BACKUP))
    private val notificationHandler:
            NotificationHandler<NotificationCompat.Builder> by inject(named(Names.NOTIFICATION_HANDLER_BACKUP))

    private val backupContactsUseCase: BackupContactsUseCase by inject()
    private val backupCallLogUseCase: BackupCallLogUseCase by inject()
    private val backupSmsUseCase: BackupSmsUseCase by inject()

    private val backupLog by lazy { File(this.cacheDir, BACKUP_LOG) }
    private val backupErrorLog by lazy { File(this.cacheDir, BACKUP_ERROR_LOG) }

    private lateinit var logWriter: TextWriter<String>
    private lateinit var errorWriter: TextWriter<String>

    private var backupJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        Timber.i("Start foreground")
        notificationHandler.setup()
        startForeground(
            NOTIFICATION_ID_BACKUP_ONGOING,
            notificationHandler.getInitialNotification().build()
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        when (intent?.action) {
            ACTION_START_BACKUP -> {
                intent.getStringExtra(EXTRA_BACKUP_ROOT)?.takeIf { it.isNotBlank() }?.run {
                    backupJob = startBackup(this)
                }
            }
            ACTION_CANCEL_BACKUP -> {
                cancelBackup()
            }
        }


        return super.onStartCommand(intent, flags, startId)
    }

    private fun startBackup(
        backupRoot: String,
    ): Job {
        return lifecycleScope.launch(Dispatchers.IO) {
            Timber.i("backup - setup")
            setup()

            notificationHandler.listenAtSafeIntervals()

            Timber.i("backup - start")

            runBackupStage(
                backupRoot = backupRoot,
                shouldRun = repository::shouldBackupContacts,
                backupItems = repository.stagedContacts,
                backupBody = backupContactsUseCase::invoke,
                progressType = Progress.ProgressType.CONTACTS_BACKUP,
                errorMessage = { "Contacts backup exception: ${it.message}" },
            )

            Timber.i("backup - contacts")

            runBackupStage(
                backupRoot = backupRoot,
                shouldRun = repository::shouldBackupCalls,
                backupItems = repository.stagedCallLogs,
                backupBody = backupCallLogUseCase::invoke,
                progressType = Progress.ProgressType.CALL_LOG_BACKUP,
                errorMessage = { "Call log backup exception: ${it.message}" },
            )

            Timber.i("backup - calls")

            runBackupStage(
                backupRoot = backupRoot,
                shouldRun = repository::shouldBackupSms,
                backupItems = repository.stagedSms,
                backupBody = backupSmsUseCase::invoke,
                progressType = Progress.ProgressType.SMS_BACKUP,
                errorMessage = { "SMS backup exception: ${it.message}" },
            )

            Timber.i("backup - sms")

            emitHeadingLog(Progress.ProgressType.BACKUP_FINISHED)

            Timber.i("backup - finished")

            notificationHandler.stopListening()
            Timber.i("backup - stopListening")
            cleanup()
            Timber.i("backup - cleanup")
        }
    }

    private suspend fun setup() {
        logWriter = TextWriterImpl()
        logWriter.setup(backupLog.canonicalPath, append = true)
        errorWriter = TextWriterImpl()
        errorWriter.setup(backupErrorLog.canonicalPath, append = true)

        progressLogRepository.reset()
    }

    private suspend fun <T: ListItem> runBackupStage(
        backupRoot: String,
        shouldRun: () -> Boolean,
        backupItems: List<DataItem<T>>,
        backupBody: (String, List<DataItem<T>>) -> Flow<Progress>,
        progressType: Progress.ProgressType,
        errorMessage: (Exception) -> String,
    ) {
        try {
            if (shouldRun()) {
                emitHeadingLog(progressType)
                backupBody(backupRoot, backupItems).collect {
                    collectLogs(it)
                }
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            e.printStackTrace()
            Progress(
                progressType = progressType,
                percentage = 1.0,
                logs = errorMessage(e),
                isFailure = true,
            ).run {
                collectLogs(this)
            }
        }
    }

    private suspend fun collectLogs(
        progress: Progress,
    ) {
        if (progress.isFailure || progress.isBackupFinished()) {
            progressLogRepository.pushError(progress)
            errorWriter.writeLine(progress.logs)
        }
        progressLogRepository.pushProgress(progress)
        logWriter.writeLine(progress.logs)
    }

    private suspend fun emitHeadingLog(
        progressType: Progress.ProgressType,
    ) {
        val headingTitle = contextSource.getProgressTitle(progressType)
        collectLogs(
            progress = Progress(
                progressType = progressType,
                percentage = 1.0,
                logs = "\n${headingTitle}\n${BREAK_LINE}\n",
                isLogHeading = true,
            ),
        )
    }

    private fun cleanup() {
        if (isSetup()) {
            logWriter.close()
            errorWriter.close()
        }
        stopSelf()
    }

    private fun isSetup(): Boolean {
        return ::logWriter.isInitialized && ::errorWriter.isInitialized
    }

    private fun cancelBackup() {
        lifecycleScope.launch {
            backupJob?.cancel()
            if (isSetup()) {
                delay(1000)
                emitHeadingLog(Progress.ProgressType.BACKUP_CANCELLED)
            }
            delay(1000)
            cleanup()
        }
    }
}