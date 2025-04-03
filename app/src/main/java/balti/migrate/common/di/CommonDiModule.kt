package balti.migrate.common.di

import balti.migrate.common.data.sources.fileSystem.DirectoryBrowserImpl
import balti.migrate.common.data.sources.fileSystem.FileSystemSourceImpl
import balti.migrate.common.data.sources.fileSystem.TextWriterImpl
import balti.migrate.restore.ui.screens.BrowseRestoreDirectoryViewModel
import baltiapps.migrate.domain.common.sources.fileSystem.DirectoryBrowser
import baltiapps.migrate.domain.common.sources.fileSystem.FileSystemSource
import baltiapps.migrate.domain.common.sources.fileSystem.TextWriter
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module

enum class Names {
    TEXT_WRITER,
}

val commonDiModule = module {

    single<TextWriter<String>>(named(Names.TEXT_WRITER)) {
        TextWriterImpl()
    }

    single<FileSystemSource> {
        FileSystemSourceImpl()
    }

    singleOf(::DirectoryBrowserImpl) bind DirectoryBrowser::class

    viewModelOf(::BrowseRestoreDirectoryViewModel)

}