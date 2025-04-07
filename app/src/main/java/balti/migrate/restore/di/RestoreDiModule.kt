package balti.migrate.restore.di

import balti.migrate.common.data.repository.ProgressLogRepositoryImpl
import balti.migrate.restore.data.sources.RestoreNotificationHandlerImpl
import balti.migrate.common.data.model.CallLogData
import balti.migrate.common.data.model.SmsData
import balti.migrate.restore.data.sources.callLog.CallLogDBReader
import balti.migrate.restore.data.sources.callLog.CallLogRestore
import balti.migrate.restore.data.sources.sms.SmsDBReader
import balti.migrate.restore.data.sources.sms.SmsRestore
import balti.migrate.restore.ui.screens.browseRestoreDirectory.BrowseRestoreDirectoryViewModel
import baltiapps.migrate.domain.common.repository.ProgressLogRepository
import baltiapps.migrate.domain.common.sources.NotificationHandler
import baltiapps.migrate.domain.common.sources.fileSystem.DBReader
import baltiapps.migrate.domain.restore.repository.RestoreDataRepository
import baltiapps.migrate.domain.restore.sources.DataRestore
import baltiapps.migrate.domain.restore.usecase.ReadCallLogForRestoreUseCase
import baltiapps.migrate.domain.restore.usecase.ReadFilesFromBackupUseCase
import baltiapps.migrate.domain.restore.usecase.ReadSmsForRestoreUseCase
import baltiapps.migrate.domain.restore.usecase.RestoreCallLogUseCase
import baltiapps.migrate.domain.restore.usecase.RestoreSmsUseCase
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.core.qualifier.named
import org.koin.dsl.module

enum class Names {
    CALL_LOG_DB_READER,
    CALL_LOG_RESTORE_SOURCE,
    SMS_DB_READER,
    SMS_RESTORE_SOURCE,
    PROGRESS_LOG_REPOSITORY_RESTORE,
    NOTIFICATION_HANDLER_RESTORE,
}

val restoreDiModule = module {

    /* Sources */

    single<DBReader<CallLogData>>(named(Names.CALL_LOG_DB_READER)) {
        CallLogDBReader(get())
    }
    single<DataRestore<CallLogData>>(named(Names.CALL_LOG_RESTORE_SOURCE)) {
        CallLogRestore(get(), get())
    }
    single<DBReader<SmsData>>(named(Names.SMS_DB_READER)) {
        SmsDBReader(get())
    }
    single<DataRestore<SmsData>>(named(Names.SMS_RESTORE_SOURCE)) {
        SmsRestore(get(), get())
    }
    single<NotificationHandler<*>>(named(Names.NOTIFICATION_HANDLER_RESTORE)) {
        RestoreNotificationHandlerImpl(
            context = get(),
            notificationUtils = get(),
            contextSource = get(),
            progressLogRepository = get(named(Names.PROGRESS_LOG_REPOSITORY_RESTORE))
        )
    }

    /* Repositories */

    singleOf(::RestoreDataRepository)
    single<ProgressLogRepository>(named(Names.PROGRESS_LOG_REPOSITORY_RESTORE)) {
        ProgressLogRepositoryImpl(get())
    }

    /* Use cases */

    singleOf(::ReadFilesFromBackupUseCase)
    single {
        ReadCallLogForRestoreUseCase(
            fileSystemSource = get(),
            callLogDbReader = get(named(Names.CALL_LOG_DB_READER)),
            restoreDataRepository = get()
        )
    }
    single {
        ReadSmsForRestoreUseCase(
            fileSystemSource = get(),
            smsDbReader = get(named(Names.SMS_DB_READER)),
            restoreDataRepository = get()
        )
    }
    single {
        RestoreCallLogUseCase(
            dataRestore = get(named(Names.CALL_LOG_RESTORE_SOURCE)),
            restoreDataRepository = get()
        )
    }
    single {
        RestoreSmsUseCase(
            dataRestore = get(named(Names.SMS_RESTORE_SOURCE)),
            restoreDataRepository = get()
        )
    }

    /* View models*/

    viewModelOf(::BrowseRestoreDirectoryViewModel)
}