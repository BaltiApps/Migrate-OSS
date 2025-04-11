package balti.migrate.backup.di

import balti.migrate.backup.data.sources.BackupNotificationHandlerImpl
import balti.migrate.backup.data.sources.callLog.CallLogDBWriter
import balti.migrate.backup.data.sources.callLog.REWRITE_CallLogDBWriter
import balti.migrate.backup.data.sources.callLog.CallLogSource
import balti.migrate.backup.data.sources.contacts.ContactsSource
import balti.migrate.backup.data.sources.contacts.ContactsWriter
import balti.migrate.backup.data.sources.contacts.REWRITE_ContactsDBWriter
import balti.migrate.backup.data.sources.sms.SmsDBWriter
import balti.migrate.backup.data.sources.sms.REWRITE_SmsDBWriter
import balti.migrate.backup.data.sources.sms.SmsSource
import balti.migrate.backup.ui.screens.listScreen.callLogBackup.CallLogBackupViewModel
import balti.migrate.backup.ui.screens.listScreen.contactBackup.ContactBackupViewModel
import balti.migrate.backup.ui.screens.listScreen.smsBackup.SmsBackupViewModel
import balti.migrate.backup.ui.screens.progressScreen.ProgressScreenViewModel
import balti.migrate.common.data.model.CallLogData
import balti.migrate.common.data.model.ContactData
import balti.migrate.common.data.model.SmsData
import balti.migrate.common.data.repository.ProgressLogRepositoryImpl
import baltiapps.migrate.domain.backup.repository.BackupDataRepository
import baltiapps.migrate.domain.backup.sources.DataSource
import baltiapps.migrate.domain.backup.usecase.REWRITE_BackupCallLogUseCase
import baltiapps.migrate.domain.backup.usecase.REWRITE_BackupContactsUseCase
import baltiapps.migrate.domain.backup.usecase.REWRITE_BackupSmsUseCase
import baltiapps.migrate.domain.backup.usecase.ReadCallLogUseCase
import baltiapps.migrate.domain.backup.usecase.ReadContactsUseCase
import baltiapps.migrate.domain.backup.usecase.ReadSmsUseCase
import baltiapps.migrate.domain.backup.usecase.StageSelectedContacts
import baltiapps.migrate.domain.common.repository.ProgressLogRepository
import baltiapps.migrate.domain.common.sources.NotificationHandler
import baltiapps.migrate.domain.common.sources.fileSystem.DBWriter
import baltiapps.migrate.domain.common.sources.fileSystem.REWRITE_DBWriter
import baltiapps.migrate.domain.common.sources.fileSystem.TextWriter
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.core.qualifier.named
import org.koin.dsl.module

enum class Names {
    CONTACTS_SOURCE,
    CALL_LOG_SOURCE,
    SMS_SOURCE,
    DB_WRITER_CALL_LOG,
    REWRITE_DB_WRITER_CALL_LOG,
    DB_WRITER_SMS,
    REWRITE_DB_WRITER_SMS,
    CONTACTS_WRITER,
    REWRITE_CONTACTS_WRITER,
    PROGRESS_LOG_REPOSITORY_BACKUP,
    NOTIFICATION_HANDLER_BACKUP,
}

val backupDiModule = module {

    /* Sources */

    single<DataSource<ContactData>>(named(Names.CONTACTS_SOURCE)) {
        ContactsSource(get(), get())
    }
    single<TextWriter<ContactData>>(named(Names.CONTACTS_WRITER)) {
        ContactsWriter()
    }
    single<REWRITE_DBWriter<ContactData>>(named(Names.REWRITE_CONTACTS_WRITER)) {
        REWRITE_ContactsDBWriter(get())
    }
    single<DataSource<CallLogData>>(named(Names.CALL_LOG_SOURCE)) {
        CallLogSource(get(), get())
    }
    single<DBWriter<CallLogData>>(named(Names.DB_WRITER_CALL_LOG)) {
        CallLogDBWriter(get())
    }
    single<REWRITE_DBWriter<CallLogData>>(named(Names.REWRITE_DB_WRITER_CALL_LOG)) {
        REWRITE_CallLogDBWriter(get())
    }
    single<DataSource<SmsData>>(named(Names.SMS_SOURCE)) {
        SmsSource(get(), get())
    }
    single<DBWriter<SmsData>>(named(Names.DB_WRITER_SMS)) {
        SmsDBWriter(get())
    }
    single<REWRITE_DBWriter<SmsData>>(named(Names.REWRITE_DB_WRITER_SMS)) {
        REWRITE_SmsDBWriter(get())
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
        ReadContactsUseCase(
            contactsSource = get(named(Names.CONTACTS_SOURCE)),
            backupDataRepository = get(),
        )
    }
    single {
        ReadCallLogUseCase(
            callLogSource = get(named(Names.CALL_LOG_SOURCE)),
            backupDataRepository = get(),
        )
    }
    single {
        ReadSmsUseCase(
            smsSource = get(named(Names.SMS_SOURCE)),
            backupDataRepository = get(),
        )
    }
    singleOf(::StageSelectedContacts)
    single {
        REWRITE_BackupContactsUseCase(
            fileSystemSource = get(),
            contactsDBWriter = get(named(Names.REWRITE_CONTACTS_WRITER)),
            dataRepository = get()
        )
    }
    single {
        REWRITE_BackupCallLogUseCase(
            fileSystemSource = get(),
            callLogDBWriter = get(named(Names.REWRITE_DB_WRITER_CALL_LOG)),
            dataRepository = get()
        )
    }
    single {
        REWRITE_BackupSmsUseCase(
            fileSystemSource = get(),
            smsDBWriter = get(named(Names.REWRITE_DB_WRITER_SMS)),
            dataRepository = get()
        )
    }

    /* ViewModels */

    viewModelOf(::ContactBackupViewModel)
    viewModelOf(::CallLogBackupViewModel)
    viewModelOf(::SmsBackupViewModel)
    viewModel {
        ProgressScreenViewModel(
            contextSource = get(),
            progressLogRepository = get(named(Names.PROGRESS_LOG_REPOSITORY_BACKUP))
        )
    }
}