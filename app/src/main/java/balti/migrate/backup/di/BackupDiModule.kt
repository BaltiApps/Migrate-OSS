package balti.migrate.backup.di

import balti.migrate.common.model.CallLogData
import balti.migrate.backup.data.model.ContactData
import balti.migrate.backup.data.model.SmsData
import balti.migrate.backup.data.repository.BackupProgressLogRepositoryImpl
import balti.migrate.backup.data.sources.ContextSourceImpl
import balti.migrate.backup.data.sources.NotificationHandlerImpl
import balti.migrate.backup.data.sources.callLog.CallLogDBWriter
import balti.migrate.backup.data.sources.callLog.CallLogSource
import balti.migrate.backup.data.sources.contacts.ContactsSource
import balti.migrate.backup.data.sources.contacts.ContactsWriter
import balti.migrate.backup.data.sources.sms.SmsDBWriter
import balti.migrate.backup.data.sources.sms.SmsSource
import balti.migrate.backup.ui.screens.listScreen.callLogBackup.CallLogBackupViewModel
import balti.migrate.backup.ui.screens.listScreen.contactBackup.ContactBackupViewModel
import balti.migrate.backup.ui.screens.listScreen.smsBackup.SmsBackupViewModel
import balti.migrate.backup.ui.screens.progressScreen.ProgressScreenViewModel
import baltiapps.migrate.domain.backup.repository.BackupProgressLogRepository
import baltiapps.migrate.domain.backup.repository.DataRepository
import baltiapps.migrate.domain.backup.sources.ContextSource
import baltiapps.migrate.domain.common.sources.fileSystem.DBWriter
import baltiapps.migrate.domain.backup.sources.DataSource
import baltiapps.migrate.domain.backup.sources.NotificationHandler
import baltiapps.migrate.domain.common.sources.fileSystem.TextWriter
import baltiapps.migrate.domain.backup.usecase.BackupCallLogUseCase
import baltiapps.migrate.domain.backup.usecase.BackupContactsUseCase
import baltiapps.migrate.domain.backup.usecase.BackupSmsUseCase
import baltiapps.migrate.domain.backup.usecase.ReadCallLogUseCase
import baltiapps.migrate.domain.backup.usecase.ReadContactsUseCase
import baltiapps.migrate.domain.backup.usecase.ReadSmsUseCase
import baltiapps.migrate.domain.backup.usecase.StageSelectedCallLogs
import baltiapps.migrate.domain.backup.usecase.StageSelectedContacts
import baltiapps.migrate.domain.backup.usecase.StageSelectedSms
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module

private enum class Names {
    CONTACTS_SOURCE,
    CALL_LOG_SOURCE,
    SMS_SOURCE,
    DB_WRITER_CALL_LOG,
    DB_WRITER_SMS,
    CONTACTS_WRITER,
}

val backupDiModule = module {

    single<DataSource<ContactData>>(named(Names.CONTACTS_SOURCE)) {
        ContactsSource(get())
    }

    single<DataSource<CallLogData>>(named(Names.CALL_LOG_SOURCE)) {
        CallLogSource(get())
    }

    single<DataSource<SmsData>>(named(Names.SMS_SOURCE)) {
        SmsSource(get())
    }

    single<TextWriter<ContactData>>(named(Names.CONTACTS_WRITER)) {
        ContactsWriter()
    }

    single<DBWriter<CallLogData>>(named(Names.DB_WRITER_CALL_LOG)) {
        CallLogDBWriter()
    }

    single<DBWriter<SmsData>>(named(Names.DB_WRITER_SMS)) {
        SmsDBWriter()
    }

    singleOf(::ContextSourceImpl) { bind<ContextSource>() }

    singleOf(::NotificationHandlerImpl) { bind<NotificationHandler<*>>() }

    singleOf(::DataRepository)

    single {
        ReadContactsUseCase(
            contactsSource = get(named(Names.CONTACTS_SOURCE)),
            dataRepository = get(),
        )
    }
    single {
        ReadCallLogUseCase(
            callLogSource = get(named(Names.CALL_LOG_SOURCE)),
            dataRepository = get(),
        )
    }
    single {
        ReadSmsUseCase(
            smsSource = get(named(Names.SMS_SOURCE)),
            dataRepository = get(),
        )
    }

    singleOf(::StageSelectedCallLogs)
    singleOf(::StageSelectedContacts)
    singleOf(::StageSelectedSms)

    viewModelOf(::ContactBackupViewModel)

    viewModelOf(::CallLogBackupViewModel)

    viewModelOf(::SmsBackupViewModel)

    singleOf(::BackupProgressLogRepositoryImpl) bind BackupProgressLogRepository::class

    viewModelOf(::ProgressScreenViewModel)

    single {
        BackupContactsUseCase(
            fileSystemSource = get(),
            contactsWriter = get(named(Names.CONTACTS_WRITER))
        )
    }

    single {
        BackupCallLogUseCase(
            fileSystemSource = get(),
            callLogDBWriter = get(named(Names.DB_WRITER_CALL_LOG)),
        )
    }

    single {
        BackupSmsUseCase(
            fileSystemSource = get(),
            smsDBWriter = get(named(Names.DB_WRITER_SMS))
        )
    }
}