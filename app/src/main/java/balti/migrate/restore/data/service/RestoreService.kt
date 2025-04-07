package balti.migrate.restore.data.service

import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import balti.migrate.common.data.model.NotificationInfo
import balti.migrate.common.data.sources.fileSystem.TextWriterImpl
import balti.migrate.common.utils.ServiceUtils
import balti.migrate.restore.di.Names
import baltiapps.migrate.domain.ACTION_CANCEL_RESTORE
import baltiapps.migrate.domain.ACTION_START_RESTORE
import baltiapps.migrate.domain.RESTORE_ERROR_LOG
import baltiapps.migrate.domain.RESTORE_LOG
import baltiapps.migrate.domain.common.model.Progress
import baltiapps.migrate.domain.common.repository.ProgressLogRepository
import baltiapps.migrate.domain.common.sources.ContextSource
import baltiapps.migrate.domain.common.sources.NotificationHandler
import baltiapps.migrate.domain.common.sources.fileSystem.TextWriter
import baltiapps.migrate.domain.restore.repository.RestoreDataRepository
import baltiapps.migrate.domain.restore.usecase.RestoreCallLogUseCase
import baltiapps.migrate.domain.restore.usecase.RestoreSmsUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.core.qualifier.named
import timber.log.Timber
import java.io.File

class RestoreService: LifecycleService() {

    private val contextSource: ContextSource by inject()
    private val repository: RestoreDataRepository by inject()
    private val progressLogRepository: ProgressLogRepository by inject(named(Names.PROGRESS_LOG_REPOSITORY_RESTORE))
    private val notificationHandler:
            NotificationHandler<NotificationInfo> by inject(named(Names.NOTIFICATION_HANDLER_RESTORE))

    private val restoreCallLogUseCase: RestoreCallLogUseCase by inject()
    private val restoreSmsUseCase: RestoreSmsUseCase by inject()

    private val restoreLog by lazy { File(this.cacheDir, RESTORE_LOG) }
    private val restoreErrorLog by lazy { File(this.cacheDir, RESTORE_ERROR_LOG) }

    private lateinit var logWriter: TextWriter<String>
    private lateinit var errorWriter: TextWriter<String>

    private lateinit var serviceUtils: ServiceUtils

    private var restoreJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        Timber.i("Start foreground restore service")
        notificationHandler.setup()
        val initialNotification = notificationHandler.getInitialNotification()
        startForeground(
            initialNotification.notificationId,
            initialNotification.convertToNotificationBuilder(this).apply {
                // https://stackoverflow.com/a/73074884/10967630
                setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            }.build()
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        when (intent?.action) {
            ACTION_START_RESTORE -> {
                restoreJob = startRestore()
            }
            ACTION_CANCEL_RESTORE -> {
                cancelRestore()
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun startRestore(): Job {
        return lifecycleScope.launch(Dispatchers.IO) {
            Timber.i("restore - setup")
            setup()

            notificationHandler.listenAtSafeIntervals()

            Timber.i("restore - start - call logs")

            serviceUtils.runStage(
                shouldRun = repository::shouldRestoreCallLogs,
                stageBody = restoreCallLogUseCase::invoke,
                progressType = Progress.ProgressType.CALL_LOG_RESTORE,
                errorMessage = { "Call log restore exception: ${it.message}" },
            )

            Timber.i("restore - finished - call logs")

            Timber.i("restore - start - sms")

            serviceUtils.runStage(
                shouldRun = repository::shouldRestoreSms,
                stageBody = restoreSmsUseCase::invoke,
                progressType = Progress.ProgressType.SMS_RESTORE,
                errorMessage = { "SMS restore exception: ${it.message}" },
            )

            Timber.i("restore - finished - sms")

            serviceUtils.emitHeadingLog(Progress.ProgressType.RESTORE_FINISHED)

            Timber.i("restore - finished")

            notificationHandler.stopListening()
            Timber.i("restore - notification handler stopped listening")
            cleanup()
            Timber.i("restore - cleanup done")
        }
    }

    private suspend fun setup() {
        logWriter = TextWriterImpl()
        logWriter.setup(restoreLog.canonicalPath, append = true)
        errorWriter = TextWriterImpl()
        errorWriter.setup(restoreErrorLog.canonicalPath, append = true)

        progressLogRepository.reset()

        serviceUtils = ServiceUtils(
            contextSource = contextSource,
            progressLogRepository = progressLogRepository,
            logWriter = logWriter,
            errorWriter = errorWriter,
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

    private fun cancelRestore() {
        lifecycleScope.launch {
            restoreJob?.cancel()
            if (isSetup()) {
                delay(1000)
                serviceUtils.emitHeadingLog(Progress.ProgressType.RESTORE_CANCELLED)
            }
            delay(1000)
            cleanup()
        }
    }

}