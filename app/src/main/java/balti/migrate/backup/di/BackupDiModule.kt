package balti.migrate.backup.di

import balti.migrate.backup.data.sources.BackupNotificationHandlerImpl
import balti.migrate.backup.data.sources.apps.AppBackupEngine
import balti.migrate.backup.data.sources.apps.ExternalDataBackupEngine
import balti.migrate.backup.data.sources.apps.AppIconWriter
import balti.migrate.backup.data.sources.apps.AppInfoWriter
import balti.migrate.backup.data.sources.apps.AppListSource
import balti.migrate.backup.data.sources.apps.AppSizeReader
import balti.migrate.backup.data.sources.callLog.CallLogBackupEngine
import balti.migrate.backup.data.sources.callLog.CallLogSource
import balti.migrate.backup.data.sources.contacts.ContactsBackupEngine
import balti.migrate.backup.data.sources.contacts.ContactsSource
import balti.migrate.backup.data.sources.sms.SmsBackupEngine
import balti.migrate.backup.data.sources.sms.SmsSource
import balti.migrate.backup.ui.screens.backupName.BackupNameViewModel
import balti.migrate.backup.ui.screens.listScreen.appBackupSelection.AppBackupSelectionViewModel
import balti.migrate.backup.ui.screens.listScreen.extraOptions.ExtraOptionsViewModel
import balti.migrate.backup.ui.screens.listScreen.extraOptions.externalData.ExternalDataViewModel
import balti.migrate.backup.ui.screens.listScreen.callLogBackupSelection.CallLogBackupSelectionViewModel
import balti.migrate.backup.ui.screens.listScreen.contactBackupSelection.ContactBackupSelectionViewModel
import balti.migrate.backup.ui.screens.listScreen.smsBackupSelection.SmsBackupSelectionViewModel
import balti.migrate.backup.ui.screens.progressScreen.BackupProgressScreenViewModel
import balti.migrate.common.data.model.AppData
import balti.migrate.common.data.model.CallLogData
import balti.migrate.common.data.model.ContactData
import balti.migrate.common.data.model.SmsData
import balti.migrate.common.data.repository.ProgressLogRepositoryImpl
import baltiapps.migrate.domain.backup.repository.BackupDataRepository
import baltiapps.migrate.domain.backup.sources.BackupEngine
import baltiapps.migrate.domain.backup.sources.DataSource
import baltiapps.migrate.domain.backup.usecase.BackupAppsInfoUseCase
import baltiapps.migrate.domain.backup.usecase.BackupAppsUseCase
import baltiapps.migrate.domain.backup.usecase.BackupExternalDataUseCase
import baltiapps.migrate.domain.backup.usecase.BackupCallLogUseCase
import baltiapps.migrate.domain.backup.usecase.BackupContactsUseCase
import baltiapps.migrate.domain.backup.usecase.BackupSmsUseCase
import baltiapps.migrate.domain.backup.usecase.ReadAppListForBackupUseCase
import baltiapps.migrate.domain.backup.usecase.ReadCallLogForBackupUseCase
import baltiapps.migrate.domain.backup.usecase.ReadContactsForBackupUseCase
import baltiapps.migrate.domain.backup.usecase.ReadSmsForBackupUseCase
import baltiapps.migrate.domain.common.model.AppListItem
import baltiapps.migrate.domain.common.model.AppSizeInfo
import baltiapps.migrate.domain.common.model.DataItem
import baltiapps.migrate.domain.common.repository.ProgressLogRepository
import baltiapps.migrate.domain.common.sources.NotificationHandler
import baltiapps.migrate.domain.common.sources.fileSystem.TextWriter
import baltiapps.migrate.domain.restore.usecase.CalculateStagedAppsSizesUseCase
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.core.qualifier.named
import org.koin.dsl.module

enum class Names {
    CONTACTS_SOURCE,
    CALL_LOG_SOURCE,
    SMS_SOURCE,
    APP_LIST_SOURCE,
    DB_WRITER_CONTACTS,
    DB_WRITER_CALL_LOG,
    DB_WRITER_SMS,
    APP_BACKUP_ENGINE,
    APP_EXTERNAL_DATA_BACKUP_ENGINE,
    APP_ICON_WRITER,
    APP_INFO_WRITER,
    APP_SIZE_READER,
    PROGRESS_LOG_REPOSITORY_BACKUP,
    NOTIFICATION_HANDLER_BACKUP,
}

val backupDiModule = module {

    /* Sources */

    single<DataSource<ContactData>>(named(Names.CONTACTS_SOURCE)) {
        ContactsSource(get(), get())
    }
    single<BackupEngine<ContactData>>(named(Names.DB_WRITER_CONTACTS)) {
        ContactsBackupEngine(get())
    }
    single<DataSource<CallLogData>>(named(Names.CALL_LOG_SOURCE)) {
        CallLogSource(get(), get())
    }
    single<BackupEngine<CallLogData>>(named(Names.DB_WRITER_CALL_LOG)) {
        CallLogBackupEngine(get())
    }
    single<DataSource<SmsData>>(named(Names.SMS_SOURCE)) {
        SmsSource(get(), get())
    }
    single<BackupEngine<SmsData>>(named(Names.DB_WRITER_SMS)) {
        SmsBackupEngine(get())
    }
    single<DataSource<AppData>>(named(Names.APP_LIST_SOURCE)) {
        AppListSource(get(), get())
    }

    single<DataSource<AppSizeInfo>>(named(Names.APP_SIZE_READER)) {
        AppSizeReader(
            applicationContext = get(),
            superuserUtils = get(),
            backupDataRepository = get(),
        )
    }

    single<BackupEngine<AppData>>(named(Names.APP_BACKUP_ENGINE)) {
        AppBackupEngine(
            applicationContext = get(),
            superuserUtils = get(),
        )
    }
    single<BackupEngine<DataItem<AppListItem>>>(named(Names.APP_EXTERNAL_DATA_BACKUP_ENGINE)) {
        ExternalDataBackupEngine(
            applicationContext = get(),
            superuserUtils = get(),
        )
    }
    single<TextWriter<AppData>>(named(Names.APP_ICON_WRITER)) {
        AppIconWriter()
    }
    single<TextWriter<AppData>>(named(Names.APP_INFO_WRITER)) {
        AppInfoWriter()
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
        ReadAppListForBackupUseCase(
            appListSource = get(named(Names.APP_LIST_SOURCE)),
            backupDataRepository = get(),
        )
    }
    single {
        BackupContactsUseCase(
            contactsBackupEngine = get(named(Names.DB_WRITER_CONTACTS)),
            dataRepository = get()
        )
    }
    single {
        BackupCallLogUseCase(
            callLogBackupEngine = get(named(Names.DB_WRITER_CALL_LOG)),
            dataRepository = get()
        )
    }
    single {
        BackupSmsUseCase(
            smsBackupEngine = get(named(Names.DB_WRITER_SMS)),
            dataRepository = get()
        )
    }
    single {
        BackupAppsInfoUseCase(
            appIconWriter = get(named(Names.APP_ICON_WRITER)),
            appInfoWriter = get(named(Names.APP_INFO_WRITER)),
            appDataItemAppSizeInfoMerger = get(),
            dataRepository = get(),
        )
    }
    single {
        BackupAppsUseCase(
            appBackupEngine = get(named(Names.APP_BACKUP_ENGINE)),
            dataRepository = get(),
        )
    }
    single {
        BackupExternalDataUseCase(
            externalDataBackupEngine = get(named(Names.APP_EXTERNAL_DATA_BACKUP_ENGINE)),
            dataRepository = get(),
        )
    }
    single {
        CalculateStagedAppsSizesUseCase(
            backupDataRepository = get(),
            appSizeReader = get(named(Names.APP_SIZE_READER)),
        )
    }

    /* ViewModels */

    viewModelOf(::ContactBackupSelectionViewModel)
    viewModelOf(::CallLogBackupSelectionViewModel)
    viewModelOf(::SmsBackupSelectionViewModel)
    viewModelOf(::AppBackupSelectionViewModel)
    viewModelOf(::ExtraOptionsViewModel)
    viewModelOf(::ExternalDataViewModel)
    viewModelOf(::BackupNameViewModel)
    viewModel {
        BackupProgressScreenViewModel(
            contextSource = get(),
            preferences = get(),
            progressLogRepository = get(named(Names.PROGRESS_LOG_REPOSITORY_BACKUP))
        )
    }
}
