package balti.migrate.common.di

import balti.migrate.common.data.converter.AppListItemToDataItemConverterImpl
import balti.migrate.common.data.sources.ContextSourceImpl
import balti.migrate.common.data.sources.PreferencesImpl
import balti.migrate.common.data.sources.fileSystem.ExportDirectoryBrowserMediaStore
import balti.migrate.common.data.sources.fileSystem.ExportDirectoryBrowserSafFile
import balti.migrate.common.data.sources.fileSystem.FileSystemSourceImpl
import balti.migrate.common.data.sources.fileSystem.TextWriterImpl
import balti.migrate.common.utils.DBUtils
import balti.migrate.common.utils.ListItemUtils
import balti.migrate.common.utils.SuperuserUtils
import baltiapps.migrate.domain.common.converter.AppListItemToDataItemConverter
import baltiapps.migrate.domain.common.sources.ContextSource
import baltiapps.migrate.domain.common.sources.Preferences
import baltiapps.migrate.domain.common.sources.fileSystem.ExportDirectoryBrowser
import baltiapps.migrate.domain.common.sources.fileSystem.FileSystemSource
import baltiapps.migrate.domain.common.sources.fileSystem.TextWriter
import baltiapps.migrate.domain.common.usecase.GetRequiredSpaceUseCase
import baltiapps.migrate.domain.common.usecase.StageSelectedApps
import baltiapps.migrate.domain.common.usecase.StageSelectedCallLogs
import baltiapps.migrate.domain.common.usecase.StageSelectedContacts
import baltiapps.migrate.domain.common.usecase.StageSelectedSms
import org.koin.core.module.dsl.singleOf
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module

enum class Names {
    TEXT_WRITER,
    MEDIA_STORE_EXPORT_DIRECTORY_BROWSER,
    SAF_EXPORT_DIRECTORY_BROWSER,
}

val commonDiModule = module {

    /* Utils */

    singleOf(::DBUtils)
    singleOf(::ListItemUtils)
    singleOf(::SuperuserUtils)

    /* Sources */

    single<TextWriter<String>>(named(Names.TEXT_WRITER)) {
        TextWriterImpl()
    }

    singleOf(::FileSystemSourceImpl) bind FileSystemSource::class

    singleOf(::ContextSourceImpl) bind ContextSource::class

    singleOf(::PreferencesImpl) bind Preferences::class

    single<ExportDirectoryBrowser<*>>(named(Names.MEDIA_STORE_EXPORT_DIRECTORY_BROWSER)) {
        ExportDirectoryBrowserMediaStore(
            applicationContext = get(),
            dbUtils = get(),
        )
    }

    single<ExportDirectoryBrowser<*>>(named(Names.SAF_EXPORT_DIRECTORY_BROWSER)) {
        ExportDirectoryBrowserSafFile(
            applicationContext = get(),
        )
    }

    single<AppListItemToDataItemConverter<*>> {
        AppListItemToDataItemConverterImpl()
    }

    /* Use cases */

    singleOf(::StageSelectedContacts)
    singleOf(::StageSelectedCallLogs)
    singleOf(::StageSelectedSms)
    singleOf(::StageSelectedApps)
    singleOf(::GetRequiredSpaceUseCase)
}