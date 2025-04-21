package balti.migrate.backup.di

import balti.migrate.backup.data.sources.BackupNotificationHandlerImpl
import balti.migrate.backup.data.sources.callLog.CallLogDBWriter
import balti.migrate.backup.data.sources.callLog.CallLogSource
import balti.migrate.backup.data.sources.contacts.ContactsSource
import balti.migrate.backup.data.sources.contacts.ContactsDBWriter
import balti.migrate.backup.data.sources.sms.SmsDBWriter
import balti.migrate.backup.data.sources.sms.SmsSource
import balti.migrate.backup.ui.screens.backupName.BackupNameViewModel
import balti.migrate.backup.ui.screens.listScreen.callLogBackupSelection.CallLogBackupSelectionViewModel
import balti.migrate.backup.ui.screens.listScreen.contactBackupSelection.ContactBackupSelectionViewModel
import balti.migrate.backup.ui.screens.listScreen.smsBackupSelection.SmsBackupSelectionViewModel
import balti.migrate.backup.ui.screens.progressScreen.BackupProgressScreenViewModel
import balti.migrate.common.data.model.CallLogData
import balti.migrate.common.data.model.ContactData
import balti.migrate.common.data.model.SmsData
import balti.migrate.common.data.repository.ProgressLogRepositoryImpl
import baltiapps.migrate.domain.backup.repository.BackupDataRepository
import baltiapps.migrate.domain.backup.sources.DataSource
import baltiapps.migrate.domain.backup.usecase.BackupCallLogUseCase
import baltiapps.migrate.domain.backup.usecase.BackupContactsUseCase
import baltiapps.migrate.domain.backup.usecase.BackupSmsUseCase
import baltiapps.migrate.domain.backup.usecase.ReadCallLogForBackupUseCase
import baltiapps.migrate.domain.backup.usecase.ReadContactsForBackupUseCase
import baltiapps.migrate.domain.backup.usecase.ReadSmsForBackupUseCase
import baltiapps.migrate.domain.common.repository.ProgressLogRepository
import baltiapps.migrate.domain.common.sources.NotificationHandler
import baltiapps.migrate.domain.common.sources.fileSystem.DBWriter
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.core.qualifier.named
import org.koin.dsl.module

enum class Names {
    CONTACTS_SOURCE,
    CALL_LOG_SOURCE,
    SMS_SOURCE,
    DB_WRITER_CONTACTS,
    DB_WRITER_CALL_LOG,
    DB_WRITER_SMS,
    PROGRESS_LOG_REPOSITORY_BACKUP,
    NOTIFICATION_HANDLER_BACKUP,
}

val backupDiModule = module {

    /* Sources */

    single<DataSource<ContactData>>(named(Names.CONTACTS_SOURCE)) {
        ContactsSource(get(), get())
    }
    single<DBWriter<ContactData>>(named(Names.DB_WRITER_CONTACTS)) {
        ContactsDBWriter(get())
    }
    single<DataSource<CallLogData>>(named(Names.CALL_LOG_SOURCE)) {
        CallLogSource(get(), get())
    }
    single<DBWriter<CallLogData>>(named(Names.DB_WRITER_CALL_LOG)) {
        CallLogDBWriter(get())
    }
    single<DataSource<SmsData>>(named(Names.SMS_SOURCE)) {
        SmsSource(get(), get())
    }
    single<DBWriter<SmsData>>(named(Names.DB_WRITER_SMS)) {
        SmsDBWriter(get())
    }
    single<NotificationHandler<*>>(named(Names.NOTIFICATION_HANDLER_BACKUP)) {
        BackupNotificationHandlerImpl(
            context = get(),
            contextSource = get(),
            progressLogRepository = get(named(Names.PROGRESS_LOG_REPOSITORY_BACKUP))
        )
    }

    /* Repositories */

    singleOf(::BackupDataRepository)
    single<ProgressLogRepository>(named(Names.PROGRESS_LOG_REPOSITORY_BACKUP)) {
        ProgressLogRepositoryImpl(get())
    }

    /* Use cases */

    single {
        ReadContactsForBackupUseCase(
            contactsSource = get(named(Names.CONTACTS_SOURCE)),
            backupDataRepository = get(),
        )
    }
    single {
        ReadCallLogForBackupUseCase(
            callLogSource = get(named(Names.CALL_LOG_SOURCE)),
            backupDataRepository = get(),
        )
    }
    single {
        ReadSmsForBackupUseCase(
            smsSource = get(named(Names.SMS_SOURCE)),
            backupDataRepository = get(),
        )
    }
    single {
        BackupContactsUseCase(
            fileSystemSource = get(),
            contactsDBWriter = get(named(Names.DB_WRITER_CONTACTS)),
            dataRepository = get()
        )
    }
    single {
        BackupCallLogUseCase(
            fileSystemSource = get(),
            callLogDBWriter = get(named(Names.DB_WRITER_CALL_LOG)),
            dataRepository = get()
        )
    }
    single {
        BackupSmsUseCase(
            fileSystemSource = get(),
            smsDBWriter = get(named(Names.DB_WRITER_SMS)),
            dataRepository = get()
        )
    }

    /* ViewModels */

    viewModelOf(::ContactBackupSelectionViewModel)
    viewModelOf(::CallLogBackupSelectionViewModel)
    viewModelOf(::SmsBackupSelectionViewModel)
    viewModelOf(::BackupNameViewModel)
    viewModel {
        BackupProgressScreenViewModel(
            contextSource = get(),
            progressLogRepository = get(named(Names.PROGRESS_LOG_REPOSITORY_BACKUP))
        )
    }
}