package balti.migrate.common.di

import balti.migrate.common.data.sources.fileSystem.DirectoryBrowserImpl
import balti.migrate.common.data.sources.fileSystem.FileSystemSourceImpl
import balti.migrate.common.data.sources.fileSystem.TextWriterImpl
import balti.migrate.common.utils.DBUtils
import baltiapps.migrate.domain.common.sources.fileSystem.DirectoryBrowser
import baltiapps.migrate.domain.common.sources.fileSystem.FileSystemSource
import baltiapps.migrate.domain.common.sources.fileSystem.TextWriter
import org.koin.core.module.dsl.singleOf
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module

enum class Names {
    TEXT_WRITER,
}

val commonDiModule = module {

    singleOf(::DBUtils)

    single<TextWriter<String>>(named(Names.TEXT_WRITER)) {
        TextWriterImpl()
    }

    singleOf(::FileSystemSourceImpl) bind FileSystemSource::class

    singleOf(::DirectoryBrowserImpl) bind DirectoryBrowser::class
}