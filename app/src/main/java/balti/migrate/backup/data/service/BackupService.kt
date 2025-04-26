package balti.migrate.backup.data.service

import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import balti.migrate.backup.di.Names
import balti.migrate.common.data.model.JavaFile
import balti.migrate.common.data.model.NotificationInfo
import balti.migrate.common.data.sources.fileSystem.TextWriterImpl
import balti.migrate.common.utils.ServiceUtils
import baltiapps.migrate.domain.ACTION_CANCEL_BACKUP
import baltiapps.migrate.domain.ACTION_START_BACKUP
import baltiapps.migrate.domain.BACKUP_ERROR_LOG
import baltiapps.migrate.domain.BACKUP_FILE_NAME_CALL_LOGS
import baltiapps.migrate.domain.BACKUP_FILE_NAME_CONTACTS
import baltiapps.migrate.domain.BACKUP_FILE_NAME_SMS
import baltiapps.migrate.domain.BACKUP_LOG
import baltiapps.migrate.domain.EXTRA_BACKUP_LOCATION
import baltiapps.migrate.domain.EXTRA_BACKUP_NAME
import baltiapps.migrate.domain.backup.repository.BackupDataRepository
import baltiapps.migrate.domain.backup.usecase.BackupCallLogUseCase
import baltiapps.migrate.domain.backup.usecase.BackupContactsUseCase
import baltiapps.migrate.domain.backup.usecase.BackupSmsUseCase
import baltiapps.migrate.domain.common.model.Directory
import baltiapps.migrate.domain.common.model.Progress
import baltiapps.migrate.domain.common.repository.ProgressLogRepository
import baltiapps.migrate.domain.common.sources.ContextSource
import baltiapps.migrate.domain.common.sources.NotificationHandler
import baltiapps.migrate.domain.common.sources.fileSystem.TextWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
            NotificationHandler<NotificationInfo> by inject(named(Names.NOTIFICATION_HANDLER_BACKUP))

    private val backupContactsUseCase: BackupContactsUseCase by inject()
    private val backupCallLogUseCase: BackupCallLogUseCase by inject()
    private val backupSmsUseCase: BackupSmsUseCase by inject()

    private val backupLog by lazy { File(this.cacheDir, BACKUP_LOG) }
    private val backupErrorLog by lazy { File(this.cacheDir, BACKUP_ERROR_LOG) }

    private lateinit var logWriter: TextWriter<String>
    private lateinit var errorWriter: TextWriter<String>

    private lateinit var serviceUtils: ServiceUtils

    private var backupJob: Job? = null

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
            ACTION_START_BACKUP -> {
                val location = intent.getStringExtra(EXTRA_BACKUP_LOCATION)
                val name = intent.getStringExtra(EXTRA_BACKUP_NAME)

                if (location == null || name == null) return super.onStartCommand(intent, flags, startId)

                val directory = Directory(
                    directoryFullPath = "$location/$name",
                    basePath = location,
                    name = name,
                    creationTime = 0L,
                    parent = null,
                    isValidBackupDirectory = true,
                )

                backupJob = startBackup(directory)
            }
            ACTION_CANCEL_BACKUP -> {
                cancelBackup()
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun startBackup(
        directory: Directory,
    ): Job {
        return lifecycleScope.launch(Dispatchers.IO) {
            Timber.i("backup - setup")
            setup()

            notificationHandler.listenAtSafeIntervals()

            Timber.i("backup - start - contacts")

            val contactsBackupFile = JavaFile("${directory.directoryFullPath}/$BACKUP_FILE_NAME_CONTACTS")
            serviceUtils.runStage(
                shouldRun = repository::shouldBackupContacts,
                stageBody = { backupContactsUseCase.invoke(directory, contactsBackupFile) },
                progressType = Progress.ProgressType.CONTACTS_BACKUP,
                errorMessage = { "Contacts backup exception: ${it.message}" },
            )

            Timber.i("backup - finished - contacts")

            Timber.i("backup - start - call logs")

            val callLogBackupFile = JavaFile("${directory.directoryFullPath}/$BACKUP_FILE_NAME_CALL_LOGS")
            serviceUtils.runStage(
                shouldRun = repository::shouldBackupCallLogs,
                stageBody = { backupCallLogUseCase.invoke(directory, callLogBackupFile) },
                progressType = Progress.ProgressType.CALL_LOG_BACKUP,
                errorMessage = { "Call log backup exception: ${it.message}" },
            )

            Timber.i("backup - finished - call logs")

            Timber.i("backup - start - sms")

            val smsBackupFile = JavaFile("${directory.directoryFullPath}/$BACKUP_FILE_NAME_SMS")
            serviceUtils.runStage(
                shouldRun = repository::shouldBackupSms,
                stageBody = { backupSmsUseCase.invoke(directory, smsBackupFile) },
                progressType = Progress.ProgressType.SMS_BACKUP,
                errorMessage = { "SMS backup exception: ${it.message}" },
            )

            Timber.i("backup - finished - sms")

            serviceUtils.emitHeadingLog(Progress.ProgressType.BACKUP_FINISHED)

            Timber.i("backup - finished")

            endNotifications()
            Timber.i("restore - notification handler stopped listening")
            cleanup()
            Timber.i("restore - cleanup done")
        }
    }

    private suspend fun setup() {
        logWriter = TextWriterImpl()
        logWriter.setup(backupLog.canonicalPath, append = true)
        errorWriter = TextWriterImpl()
        errorWriter.setup(backupErrorLog.canonicalPath, append = true)

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

    private fun endNotifications() {
        notificationHandler.stopListening()
        val notificationInfo = when {
            backupJob?.isCancelled == true -> notificationHandler.getCancelledNotification()
            progressLogRepository.isAnyErrorPresent -> notificationHandler.getFinishedWithErrorNotification()
            else -> notificationHandler.getFinishedNotification()
        }
        notificationHandler.displayNotification(notificationInfo)
    }

    private fun isSetup(): Boolean {
        return ::logWriter.isInitialized && ::errorWriter.isInitialized
    }

    private fun cancelBackup() {
        lifecycleScope.launch {
            backupJob?.cancel()
            if (isSetup()) {
                delay(1000)
                serviceUtils.emitHeadingLog(Progress.ProgressType.BACKUP_CANCELLED)
            }
            endNotifications()
            delay(1000)
            cleanup()
        }
    }
}