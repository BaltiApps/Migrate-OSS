package balti.migrate.restore.di

import balti.migrate.restore.ui.screens.BrowseRestoreDirectoryViewModel
import baltiapps.migrate.domain.restore.repository.RestoreDataRepository
import baltiapps.migrate.domain.restore.usecase.ReadCallLogForRestoreUseCase
import baltiapps.migrate.domain.restore.usecase.ReadFilesFromBackupUseCase
import baltiapps.migrate.domain.restore.usecase.ReadSmsForRestoreUseCase
import baltiapps.migrate.domain.restore.usecase.RestoreCallLogUseCase
import baltiapps.migrate.domain.restore.usecase.RestoreSmsUseCase
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val restoreDiModule = module {

    viewModelOf(::BrowseRestoreDirectoryViewModel)

    singleOf(::RestoreDataRepository)

    singleOf(::ReadCallLogForRestoreUseCase)
    singleOf(::ReadFilesFromBackupUseCase)
    singleOf(::ReadSmsForRestoreUseCase)
    singleOf(::RestoreCallLogUseCase)
    singleOf(::RestoreSmsUseCase)
}