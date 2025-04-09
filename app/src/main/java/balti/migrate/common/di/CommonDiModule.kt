package balti.migrate.common.di

import balti.migrate.common.data.sources.ContextSourceImpl
import balti.migrate.common.data.sources.fileSystem.DirectoryBrowserImpl
import balti.migrate.common.data.sources.fileSystem.FileSystemSourceImpl
import balti.migrate.common.data.sources.fileSystem.TextWriterImpl
import balti.migrate.common.utils.DBUtils
import balti.migrate.common.utils.ListItemUtils
import baltiapps.migrate.domain.common.sources.ContextSource
import baltiapps.migrate.domain.common.sources.fileSystem.DirectoryBrowser
import baltiapps.migrate.domain.common.sources.fileSystem.FileSystemSource
import baltiapps.migrate.domain.common.sources.fileSystem.TextWriter
import baltiapps.migrate.domain.common.usecase.StageSelectedCallLogs
import baltiapps.migrate.domain.common.usecase.StageSelectedSms
import org.koin.core.module.dsl.singleOf
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module

enum class Names {
    TEXT_WRITER,
}

val commonDiModule = module {

    /* Utils */

    singleOf(::DBUtils)
    singleOf(::ListItemUtils)

    /* Sources */

    single<TextWriter<String>>(named(Names.TEXT_WRITER)) {
        TextWriterImpl()
    }

    singleOf(::FileSystemSourceImpl) bind FileSystemSource::class

    singleOf(::DirectoryBrowserImpl) bind DirectoryBrowser::class

    singleOf(::ContextSourceImpl) bind ContextSource::class

    /* Use cases */

    singleOf(::StageSelectedCallLogs)
    singleOf(::StageSelectedSms)
}