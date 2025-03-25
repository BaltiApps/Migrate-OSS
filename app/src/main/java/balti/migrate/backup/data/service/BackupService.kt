package balti.migrate.backup.data.service

import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import balti.migrate.R
import balti.migrate.backup.data.service.NotificationHandler.Companion.NOTIFICATION_ID_BACKUP_ONGOING
import balti.migrate.backup.data.sources.files.TextWriterImpl
import baltiapps.migrate.domain.ACTION_CANCEL_BACKUP
import baltiapps.migrate.domain.ACTION_START_BACKUP
import baltiapps.migrate.domain.BACKUP_ERROR_LOG
import baltiapps.migrate.domain.BACKUP_LOG
import baltiapps.migrate.domain.EXTRA_BACKUP_ROOT
import baltiapps.migrate.domain.backup.model.Progress
import baltiapps.migrate.domain.backup.notification.PlatformNotificationHandler
import baltiapps.migrate.domain.backup.repository.PlatformDataRepository
import baltiapps.migrate.domain.backup.sources.TextWriter
import com.google.common.collect.EvictingQueue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import timber.log.Timber
import java.io.File

class BackupService : LifecycleService() {

    companion object {
        const val LOG_CACHE = 1000

        private fun getNewProgressFlow(): MutableSharedFlow<Progress> {
            return MutableSharedFlow()
        }

        private var _backupProgress = getNewProgressFlow()
        val backupProgress get() = _backupProgress.asSharedFlow()

        private val _progressCache = EvictingQueue.create<Progress>(LOG_CACHE)
        val progressCache get() = _progressCache.toList()

        private val _errorsCache = EvictingQueue.create<Progress>(LOG_CACHE)
        val errorsCache get() = _errorsCache.toList()

        private const val BREAK_LINE = "===================================="

        var truncatedProgress = Progress.Empty
            private set
    }

    private val repository: PlatformDataRepository by inject()

    private val backupLog by lazy { File(this.cacheDir, BACKUP_LOG) }
    private val backupErrorLog by lazy { File(this.cacheDir, BACKUP_ERROR_LOG) }

    private val notificationHandler:
            PlatformNotificationHandler<NotificationCompat.Builder> by inject()

    private lateinit var logWriter: TextWriter
    private lateinit var errorWriter: TextWriter

    private var backupJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        Timber.i("Start foreground")
        notificationHandler.setup()
        startForeground(
            NOTIFICATION_ID_BACKUP_ONGOING,
            notificationHandler.getInitialNotification().build()
        )
        truncatedProgress = truncatedProgress.copy(
            logs = "${getString(R.string.older_logs_truncated)}\n$BREAK_LINE\n"
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
            setup()

            notificationHandler.listenAtSafeIntervals()

            runBackupStage(
                backupRoot = backupRoot,
                shouldRun = repository::shouldBackupContacts,
                backupBody = repository::backupContacts,
                progressType = Progress.ProgressType.CONTACTS_BACKUP,
                errorMessage = { "Contacts backup exception: ${it.message}" },
            )

            runBackupStage(
                backupRoot = backupRoot,
                shouldRun = repository::shouldBackupCalls,
                backupBody = repository::backupCalls,
                progressType = Progress.ProgressType.CALL_LOG_BACKUP,
                errorMessage = { "Call log backup exception: ${it.message}" },
            )

            runBackupStage(
                backupRoot = backupRoot,
                shouldRun = repository::shouldBackupSms,
                backupBody = repository::backupSms,
                progressType = Progress.ProgressType.SMS_BACKUP,
                errorMessage = { "SMS backup exception: ${it.message}" },
            )

            emitHeadingLog(Progress.ProgressType.BACKUP_FINISHED)

            notificationHandler.stopListening()
            cleanup()
        }
    }

    private fun setup() {
        logWriter = TextWriterImpl()
        logWriter.setup(backupLog.canonicalPath, append = true)
        errorWriter = TextWriterImpl()
        errorWriter.setup(backupErrorLog.canonicalPath, append = true)

        _backupProgress = getNewProgressFlow()
        _progressCache.clear()
        _errorsCache.clear()
    }

    private suspend fun runBackupStage(
        backupRoot: String,
        shouldRun: () -> Boolean,
        backupBody: (String) -> Flow<Progress>,
        progressType: Progress.ProgressType,
        errorMessage: (Exception) -> String,
    ) {
        try {
            if (shouldRun()) {
                emitHeadingLog(progressType)
                backupBody(backupRoot).collect {
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
        if (progress.isFailure || progress.isFinished()) {
            _errorsCache.add(progress)
            errorWriter.writeLine(progress.logs)
        }
        _progressCache.add(progress)
        logWriter.writeLine(progress.logs)
        _backupProgress.emit(progress)
    }

    private suspend fun emitHeadingLog(
        progressType: Progress.ProgressType,
    ) {
        val headingTitle = when (progressType) {
            Progress.ProgressType.CONTACTS_BACKUP -> R.string.start_contacts_backup
            Progress.ProgressType.CALL_LOG_BACKUP -> R.string.start_call_log_backup
            Progress.ProgressType.SMS_BACKUP -> R.string.start_sms_backup
            Progress.ProgressType.BACKUP_FINISHED -> R.string.backup_finished
            Progress.ProgressType.BACKUP_FINISHED_WITH_ERRORS -> R.string.backup_finished_with_errors
            Progress.ProgressType.BACKUP_CANCELLED -> R.string.backup_cancelled
            else -> R.string.loading
        }.run { getString(this) }
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