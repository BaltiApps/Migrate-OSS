package balti.migrate.common.di

import balti.migrate.common.data.sources.DirectoryBrowserImpl
import balti.migrate.restore.ui.screens.BrowseRestoreDirectoryViewModel
import baltiapps.migrate.domain.common.sources.DirectoryBrowser
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.bind
import org.koin.dsl.module

val commonDiModule = module {

    singleOf(::DirectoryBrowserImpl) bind DirectoryBrowser::class

    viewModelOf(::BrowseRestoreDirectoryViewModel)

}