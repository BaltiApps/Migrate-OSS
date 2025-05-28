package balti.migrate.app.di

import balti.migrate.app.MainActivityViewModel
import balti.migrate.app.ui.navigation.GraphViewModel
import balti.migrate.app.ui.screens.appSettings.AppSettingsViewModel
import balti.migrate.app.ui.screens.home.ScreenHomeViewModel
import balti.migrate.app.ui.screens.setupPermission.SetupPermissionScreenViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val appDiModule = module {

    /* ViewModel */

    viewModelOf(::MainActivityViewModel)

    viewModelOf(::GraphViewModel)
    viewModelOf(::SetupPermissionScreenViewModel)
    viewModelOf(::ScreenHomeViewModel)
    viewModelOf(::AppSettingsViewModel)
}