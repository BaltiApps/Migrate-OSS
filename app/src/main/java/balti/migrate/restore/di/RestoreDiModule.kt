package balti.migrate.restore.di

import balti.migrate.common.data.model.AppData
import balti.migrate.common.data.model.CallLogData
import balti.migrate.common.data.model.ContactData
import balti.migrate.common.data.model.SmsData
import balti.migrate.common.data.repository.ProgressLogRepositoryImpl
import balti.migrate.common.di.Names.MEDIA_STORE_EXPORT_DIRECTORY_BROWSER
import balti.migrate.common.di.Names.SAF_EXPORT_DIRECTORY_BROWSER
import balti.migrate.restore.data.sources.RestoreNotificationHandlerImpl
import balti.migrate.restore.data.sources.apps.AppIconReader
import balti.migrate.restore.data.sources.apps.AppInfoReader
import balti.migrate.restore.data.sources.apps.AppRestoreEngine
import balti.migrate.restore.data.sources.apps.InternalStorageSpaceReaderImpl
import balti.migrate.restore.data.sources.callLog.CallLogRestoreEngine
import balti.migrate.restore.data.sources.callLog.CallLogRestoreReader
import balti.migrate.restore.data.sources.contacts.ContactsRestoreReader
import balti.migrate.restore.data.sources.sms.SmsRestoreEngine
import balti.migrate.restore.data.sources.sms.SmsRestoreReader
import balti.migrate.restore.ui.screens.browseRestoreDirectory.BrowseRestoreDirectoryViewModel
import balti.migrate.restore.ui.screens.listScreen.appRestoreSelection.AppRestoreSelectionViewModel
import balti.migrate.restore.ui.screens.listScreen.callLogRestoreSelection.CallLogRestoreSelectionViewModel
import balti.migrate.restore.ui.screens.listScreen.contactRestoreSelection.ContactRestoreSelectionViewModel
import balti.migrate.restore.ui.screens.listScreen.smsRestoreSelection.SmsRestoreSelectionViewModel
import balti.migrate.restore.ui.screens.progressScreen.RestoreProgressScreenViewModel
import balti.migrate.restore.ui.screens.restoreSummary.RestoreSummaryViewModel
import baltiapps.migrate.domain.common.model.DrawableAsset
import baltiapps.migrate.domain.common.repository.ProgressLogRepository
import baltiapps.migrate.domain.common.sources.NotificationHandler
import baltiapps.migrate.domain.common.sources.fileSystem.TextReader
import baltiapps.migrate.domain.restore.repository.RestoreDataRepository
import baltiapps.migrate.domain.restore.sources.InternalStorageSpaceReader
import baltiapps.migrate.domain.restore.sources.RestoreEngine
import baltiapps.migrate.domain.restore.sources.RestoreReader
import baltiapps.migrate.domain.restore.usecase.ExportContactsForRestoreUseCase
import baltiapps.migrate.domain.restore.usecase.ImportFilesFromBackupUseCase
import baltiapps.migrate.domain.restore.usecase.ReadAppListForRestoreUseCase
import baltiapps.migrate.domain.restore.usecase.ReadCallLogForRestoreUseCase
import baltiapps.migrate.domain.restore.usecase.ReadContactsForRestoreUseCase
import baltiapps.migrate.domain.restore.usecase.ReadSmsForRestoreUseCase
import baltiapps.migrate.domain.restore.usecase.RestoreAppsUseCase
import baltiapps.migrate.domain.restore.usecase.RestoreCallLogUseCase
import baltiapps.migrate.domain.restore.usecase.RestoreSmsUseCase
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module

enum class Names {
    CONTACTS_DB_READER,
    CALL_LOG_DB_READER,
    CALL_LOG_RESTORE_SOURCE,
    SMS_DB_READER,
    SMS_RESTORE_SOURCE,
    APP_ICON_READER,
    APP_INFO_READER,
    APP_RESTORE_ENGINE,
    PROGRESS_LOG_REPOSITORY_RESTORE,
    NOTIFICATION_HANDLER_RESTORE,
}

val restoreDiModule = module {

    /* Sources */

    single<RestoreReader<ContactData>>(named(Names.CONTACTS_DB_READER)) {
        ContactsRestoreReader(get())
    }
    single<RestoreReader<CallLogData>>(named(Names.CALL_LOG_DB_READER)) {
        CallLogRestoreReader(get())
    }
    single<RestoreEngine<CallLogData>>(named(Names.CALL_LOG_RESTORE_SOURCE)) {
        CallLogRestoreEngine(get(), get())
    }
    single<RestoreReader<SmsData>>(named(Names.SMS_DB_READER)) {
        SmsRestoreReader(get())
    }
    single<RestoreEngine<SmsData>>(named(Names.SMS_RESTORE_SOURCE)) {
        SmsRestoreEngine(get(), get(), get())
    }
    single<TextReader<DrawableAsset>>(named(Names.APP_ICON_READER)) {
        AppIconReader()
    }
    single<TextReader<AppData>>(named(Names.APP_INFO_READER)) {
        AppInfoReader()
    }
    single<RestoreEngine<AppData>>(named(Names.APP_RESTORE_ENGINE)) {
        AppRestoreEngine(
            applicationContext = get(),
            superuserUtils = get(),
        )
    }
    single<NotificationHandler<*>>(named(Names.NOTIFICATION_HANDLER_RESTORE)) {
        RestoreNotificationHandlerImpl(
            context = get(),
            contextSource = get(),
            progressLogRepository = get(named(Names.PROGRESS_LOG_REPOSITORY_RESTORE))
        )
    }
    singleOf(::InternalStorageSpaceReaderImpl) bind InternalStorageSpaceReader::class

    /* Repositories */

    singleOf(::RestoreDataRepository)
    single<ProgressLogRepository>(named(Names.PROGRESS_LOG_REPOSITORY_RESTORE)) {
        ProgressLogRepositoryImpl(get())
    }

    /* Use cases */

    single {
        ImportFilesFromBackupUseCase(
            restoreDataRepository = get(),
            fileSystemSource = get(),
        )
    }
    single {
        ReadContactsForRestoreUseCase(
            contactsRestoreReader = get(named(Names.CONTACTS_DB_READER)),
            restoreDataRepository = get()
        )
    }
    single {
        ReadCallLogForRestoreUseCase(
            callLogRestoreReader = get(named(Names.CALL_LOG_DB_READER)),
            restoreDataRepository = get()
        )
    }
    single {
        ReadSmsForRestoreUseCase(
            smsRestoreReader = get(named(Names.SMS_DB_READER)),
            restoreDataRepository = get()
        )
    }
    single {
        RestoreCallLogUseCase(
            callLogRestoreEngine = get(named(Names.CALL_LOG_RESTORE_SOURCE)),
            restoreDataRepository = get()
        )
    }
    single {
        RestoreSmsUseCase(
            smsRestoreEngine = get(named(Names.SMS_RESTORE_SOURCE)),
            restoreDataRepository = get()
        )
    }
    single {
        ReadAppListForRestoreUseCase(
            appIconReader = get(named(Names.APP_ICON_READER)),
            appInfoReader = get(named(Names.APP_INFO_READER)),
            restoreDataRepository = get(),
            appListItemToDataItemConverter = get()
        )
    }
    single {
        RestoreAppsUseCase(
            fileSystemSource = get(),
            appRestoreEngine = get(named(Names.APP_RESTORE_ENGINE)),
            dataRepository = get(),
        )
    }
    singleOf(::ExportContactsForRestoreUseCase)

    /* View models*/

    viewModel {
        BrowseRestoreDirectoryViewModel(
            exportDirectoryBrowserMediaStore = get(named(MEDIA_STORE_EXPORT_DIRECTORY_BROWSER)),
            exportDirectoryBrowserSafFile = get(named(SAF_EXPORT_DIRECTORY_BROWSER)),
            importFilesFromBackupUseCase = get(),
            preferences = get(),
            applicationContext = get(),
        )
    }
    viewModelOf(::ContactRestoreSelectionViewModel)
    viewModelOf(::CallLogRestoreSelectionViewModel)
    viewModelOf(::SmsRestoreSelectionViewModel)
    viewModelOf(::AppRestoreSelectionViewModel)
    viewModelOf(::RestoreSummaryViewModel)
    viewModel {
        RestoreProgressScreenViewModel(
            contextSource = get(),
            preferences = get(),
            progressLogRepository = get(named(Names.PROGRESS_LOG_REPOSITORY_RESTORE))
        )
    }
}