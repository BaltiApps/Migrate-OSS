package balti.migrate.backup.data.service

import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.net.toUri
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import balti.migrate.backup.di.Names
import balti.migrate.common.data.model.JavaFile
import balti.migrate.common.data.model.MediaStoreDownloadFile
import balti.migrate.common.data.model.NotificationInfo
import balti.migrate.common.data.model.SafFile
import balti.migrate.common.data.sources.fileSystem.TextWriterImpl
import balti.migrate.common.data.sources.fileSystem.TransferUtils
import balti.migrate.common.utils.ServiceUtils
import balti.migrate.common.utils.convertToNotificationBuilder
import baltiapps.migrate.domain.ACTION_CANCEL_BACKUP
import baltiapps.migrate.domain.ACTION_START_BACKUP
import baltiapps.migrate.domain.BACKUP_ERROR_LOG
import baltiapps.migrate.domain.BACKUP_FILE_NAME_CALL_LOGS
import baltiapps.migrate.domain.BACKUP_FILE_NAME_CONTACTS
import baltiapps.migrate.domain.BACKUP_FILE_NAME_SMS
import baltiapps.migrate.domain.BACKUP_LOG
import baltiapps.migrate.domain.EXTRA_BACKUP_NAME
import baltiapps.migrate.domain.EXTRA_BACKUP_URI_STRING
import baltiapps.migrate.domain.INTERNAL_ROUGH_WORK_BACKUP_DIRECTORY
import baltiapps.migrate.domain.backup.repository.BackupDataRepository
import baltiapps.migrate.domain.backup.usecase.BackupAppsInfoUseCase
import baltiapps.migrate.domain.backup.usecase.BackupAppsUseCase
import baltiapps.migrate.domain.backup.usecase.BackupExternalDataUseCase
import baltiapps.migrate.domain.backup.usecase.BackupCallLogUseCase
import baltiapps.migrate.domain.backup.usecase.BackupContactsUseCase
import baltiapps.migrate.domain.backup.usecase.BackupSmsUseCase
import baltiapps.migrate.domain.common.model.GenericFile
import baltiapps.migrate.domain.common.model.Progress
import baltiapps.migrate.domain.common.repository.ProgressLogRepository
import baltiapps.migrate.domain.common.sources.ContextSource
import baltiapps.migrate.domain.common.sources.NotificationHandler
import baltiapps.migrate.domain.common.sources.Preferences
import baltiapps.migrate.domain.common.sources.fileSystem.FileSystemSource
import baltiapps.migrate.domain.common.sources.fileSystem.TextWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.core.qualifier.named
import timber.log.Timber

class BackupService : LifecycleService() {

    private val contextSource: ContextSource by inject()
    private val repository: BackupDataRepository by inject()
    private val progressLogRepository: ProgressLogRepository by inject(named(Names.PROGRESS_LOG_REPOSITORY_BACKUP))
    private val notificationHandler:
            NotificationHandler<NotificationInfo> by inject(named(Names.NOTIFICATION_HANDLER_BACKUP))

    private val fileSystemSource: FileSystemSource by inject()

    private val backupContactsUseCase: BackupContactsUseCase by inject()
    private val backupCallLogUseCase: BackupCallLogUseCase by inject()
    private val backupSmsUseCase: BackupSmsUseCase by inject()

    private val backupAppsInfoUseCase: BackupAppsInfoUseCase by inject()
    private val backupAppsUseCase: BackupAppsUseCase by inject()
    private val backupExternalDataUseCase: BackupExternalDataUseCase by inject()

    private val preferences: Preferences by inject()

    private lateinit var logWriter: TextWriter<String>
    private lateinit var errorWriter: TextWriter<String>

    private lateinit var serviceUtils: ServiceUtils

    private var backupJob: Job? = null

    companion object {
        var isRunning: Boolean = false
        private set
    }

    override fun onCreate() {
        super.onCreate()
        isRunning = true
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
                val name = intent.getStringExtra(EXTRA_BACKUP_NAME)
                val backupUriString = intent.getStringExtra(EXTRA_BACKUP_URI_STRING)

                if (name == null) return super.onStartCommand(intent, flags, startId)

                backupJob = startBackup(
                    backupName = name,
                    backupUriString = backupUriString,
                )
            }
            ACTION_CANCEL_BACKUP -> {
                cancelBackup()
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun startBackup(
        backupName: String,
        backupUriString: String? = null,
    ): Job {
        return lifecycleScope.launch(Dispatchers.IO) {
            Timber.i("backup - setup")
            setup()

            val roughWorkDir = JavaFile("$filesDir/$INTERNAL_ROUGH_WORK_BACKUP_DIRECTORY")
            val internalDir = JavaFile(roughWorkDir, backupName)
            val internalDirPath = internalDir.path
            val backupDestination = if (backupUriString == null) {
                val destination = "${MediaStoreDownloadFile.EXPORT_PATH_PREFIX}/$backupName"
                MediaStoreDownloadFile(destination)
            } else {
                val locationUri = backupUriString.toUri()
                SafFile(
                    uriToLocation = locationUri,
                    name = backupName,
                    path = "${TransferUtils.getUriFilePath(locationUri)}/$backupName",
                )
            }

            roughWorkDir.file.deleteRecursively()
            fileSystemSource.createDirectory(internalDir)
            fileSystemSource.createDirectory(backupDestination)

            val backupDestinationAbsolutePath = if (backupDestination is MediaStoreDownloadFile) {
                "/sdcard/${backupDestination.path}"
            } else backupDestination.path

            notificationHandler.listenAtSafeIntervals()

            Timber.i("backup - start - contacts")

            val contactsBackupFile = JavaFile("$internalDirPath/$BACKUP_FILE_NAME_CONTACTS")
            serviceUtils.runStage(
                shouldRun = repository::shouldBackupContacts,
                stageBody = { backupContactsUseCase.invoke(contactsBackupFile) },
                progressType = Progress.ProgressType.CONTACTS_BACKUP,
                errorMessage = { "Contacts backup exception: ${it.message}" },
            )

            Timber.i("backup - finished - contacts")

            Timber.i("backup - start - call logs")

            val callLogBackupFile = JavaFile("$internalDirPath/$BACKUP_FILE_NAME_CALL_LOGS")
            serviceUtils.runStage(
                shouldRun = repository::shouldBackupCallLogs,
                stageBody = { backupCallLogUseCase.invoke(callLogBackupFile) },
                progressType = Progress.ProgressType.CALL_LOG_BACKUP,
                errorMessage = { "Call log backup exception: ${it.message}" },
            )

            Timber.i("backup - finished - call logs")

            Timber.i("backup - start - sms")

            val smsBackupFile = JavaFile("$internalDirPath/$BACKUP_FILE_NAME_SMS")
            serviceUtils.runStage(
                shouldRun = repository::shouldBackupSms,
                stageBody = { backupSmsUseCase.invoke(smsBackupFile) },
                progressType = Progress.ProgressType.SMS_BACKUP,
                errorMessage = { "SMS backup exception: ${it.message}" },
            )

            Timber.i("backup - finished - sms")

            Timber.i("backup - start - app info")

            serviceUtils.runStage(
                shouldRun = repository::shouldBackupApps,
                stageBody = { backupAppsInfoUseCase.invoke(internalDirPath) },
                progressType = Progress.ProgressType.APP_INFO_BACKUP,
                errorMessage = { "App info backup exception: ${it.message}" },
            )

            Timber.i("backup - finished - app info")

            Timber.i("backup - start - apps")

            serviceUtils.runStage(
                shouldRun = repository::shouldBackupApps,
                stageBody = { backupAppsUseCase.invoke(backupDestinationAbsolutePath) },
                progressType = Progress.ProgressType.APP_BACKUP,
                errorMessage = { "App backup exception: ${it.message}" },
            )

            Timber.i("backup - finished - apps")

            Timber.i("backup - start - external data")

            serviceUtils.runStage(
                shouldRun = repository::shouldBackupExternalData,
                stageBody = { backupExternalDataUseCase.invoke(backupDestinationAbsolutePath) },
                progressType = Progress.ProgressType.EXTERNAL_DATA_BACKUP,
                errorMessage = { "External data backup exception: ${it.message}" },
            )

            Timber.i("backup - finished - external data")

            Timber.i("backup - exporting backup")

            exportBackup(
                source = internalDir,
                destination = backupDestination,
            )

            Timber.i("backup - finished exporting backup")

            serviceUtils.emitHeadingLog(Progress.ProgressType.BACKUP_FINISHED)

            Timber.i("backup - finished")

            endNotifications()
            storeProgressAndErrorsOnFinish()
            Timber.i("restore - notification handler stopped listening")
            cleanup()
            Timber.i("restore - cleanup done")
        }
    }

    private suspend fun setup() {
        logWriter = TextWriterImpl()
        logWriter.setup(fileLocation = cacheDir.path, fileName = BACKUP_LOG, append = true)
        errorWriter = TextWriterImpl()
        errorWriter.setup(fileLocation = cacheDir.path, fileName = BACKUP_ERROR_LOG, append = true)

        progressLogRepository.reset()
        preferences.resetSavedBackupProgressList()
        preferences.resetSavedBackupErrorList()

        serviceUtils = ServiceUtils(
            contextSource = contextSource,
            progressLogRepository = progressLogRepository,
            logWriter = logWriter,
            errorWriter = errorWriter,
        )
    }

    private suspend fun exportBackup(
        source: JavaFile,
        destination: GenericFile,
    ) {
        try {
            serviceUtils.emitHeadingLog(Progress.ProgressType.EXPORTING_BACKUP)
            fileSystemSource.moveDirectory(
                source = source,
                destination = destination,
            )
            serviceUtils.collectLogs(
                Progress(
                    itemId = "",
                    progressType = Progress.ProgressType.EXPORTING_BACKUP,
                    percentage = 1.0,
                    logs = destination.path,
                    isFailure = false,
                )
            )
        } catch (e: Exception) {
            e.printStackTrace()
            serviceUtils.collectLogs(
                progress = Progress(
                    itemId = "",
                    progressType = Progress.ProgressType.EXPORTING_BACKUP,
                    percentage = 1.0,
                    logs = "Error exporting backup to ${destination.path}: ${e.message}",
                    isFailure = true,
                )
            )
        }
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

    private fun storeProgressAndErrorsOnFinish() {
        val progressList = progressLogRepository.getDisplayedProgressList()
        preferences.saveBackupProgressList(progressList)
        val errorList = progressLogRepository.getDisplayedErrorList()
        preferences.saveBackupErrorList(errorList)
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
            storeProgressAndErrorsOnFinish()
            delay(1000)
            cleanup()
        }
    }

    override fun onDestroy() {
        isRunning = false
        super.onDestroy()
    }
}