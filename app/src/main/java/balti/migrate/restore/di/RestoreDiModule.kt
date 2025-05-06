package balti.migrate.restore.di

import balti.migrate.common.data.model.CallLogData
import balti.migrate.common.data.model.ContactData
import balti.migrate.common.data.model.SmsData
import balti.migrate.common.data.repository.ProgressLogRepositoryImpl
import balti.migrate.common.di.Names.MEDIA_STORE_EXPORT_DIRECTORY_BROWSER
import balti.migrate.restore.data.sources.RestoreNotificationHandlerImpl
import balti.migrate.restore.data.sources.callLog.CallLogDBReader
import balti.migrate.restore.data.sources.callLog.CallLogRestore
import balti.migrate.restore.data.sources.contacts.ContactsDBReader
import balti.migrate.restore.data.sources.sms.SmsDBReader
import balti.migrate.restore.data.sources.sms.SmsRestore
import balti.migrate.restore.ui.screens.browseRestoreDirectory.BrowseRestoreDirectoryViewModel
import balti.migrate.restore.ui.screens.listScreen.callLogRestoreSelection.CallLogRestoreSelectionViewModel
import balti.migrate.restore.ui.screens.listScreen.contactRestoreSelection.ContactRestoreSelectionViewModel
import balti.migrate.restore.ui.screens.listScreen.smsRestoreSelection.SmsRestoreSelectionViewModel
import balti.migrate.restore.ui.screens.progressScreen.RestoreProgressScreenViewModel
import balti.migrate.restore.ui.screens.restoreSummary.RestoreSummaryViewModel
import baltiapps.migrate.domain.common.repository.ProgressLogRepository
import baltiapps.migrate.domain.common.sources.NotificationHandler
import baltiapps.migrate.domain.common.sources.fileSystem.DBReader
import baltiapps.migrate.domain.restore.repository.RestoreDataRepository
import baltiapps.migrate.domain.restore.sources.DataRestore
import baltiapps.migrate.domain.restore.usecase.ExportContactsForRestoreUseCase
import baltiapps.migrate.domain.restore.usecase.ReadCallLogForRestoreUseCase
import baltiapps.migrate.domain.restore.usecase.ReadContactsForRestoreUseCase
import baltiapps.migrate.domain.restore.usecase.ReadFilesFromBackupUseCase
import baltiapps.migrate.domain.restore.usecase.ReadSmsForRestoreUseCase
import baltiapps.migrate.domain.restore.usecase.RestoreCallLogUseCase
import baltiapps.migrate.domain.restore.usecase.RestoreSmsUseCase
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.core.qualifier.named
import org.koin.dsl.module

enum class Names {
    CONTACTS_DB_READER,
    CALL_LOG_DB_READER,
    CALL_LOG_RESTORE_SOURCE,
    SMS_DB_READER,
    SMS_RESTORE_SOURCE,
    PROGRESS_LOG_REPOSITORY_RESTORE,
    NOTIFICATION_HANDLER_RESTORE,
}

val restoreDiModule = module {

    /* Sources */

    single<DBReader<ContactData>>(named(Names.CONTACTS_DB_READER)) {
        ContactsDBReader(get())
    }
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
        SmsRestore(get(), get(), get())
    }
    single<NotificationHandler<*>>(named(Names.NOTIFICATION_HANDLER_RESTORE)) {
        RestoreNotificationHandlerImpl(
            context = get(),
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

    single {
        ReadFilesFromBackupUseCase(
            restoreDataRepository = get(),
            fileSystemSource = get(),
        )
    }
    single {
        ReadContactsForRestoreUseCase(
            fileSystemSource = get(),
            contactsDbReader = get(named(Names.CONTACTS_DB_READER)),
            restoreDataRepository = get()
        )
    }
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
    singleOf(::ExportContactsForRestoreUseCase)

    /* View models*/

    viewModel {
        BrowseRestoreDirectoryViewModel(
            exportDirectoryBrowser = get(named(MEDIA_STORE_EXPORT_DIRECTORY_BROWSER)),
            readFilesFromBackupUseCase = get(),
            applicationContext = get(),
        )
    }
    viewModelOf(::ContactRestoreSelectionViewModel)
    viewModelOf(::CallLogRestoreSelectionViewModel)
    viewModelOf(::SmsRestoreSelectionViewModel)
    viewModelOf(::RestoreSummaryViewModel)
    viewModel {
        RestoreProgressScreenViewModel(
            contextSource = get(),
            preferences = get(),
            progressLogRepository = get(named(Names.PROGRESS_LOG_REPOSITORY_RESTORE))
        )
    }
}