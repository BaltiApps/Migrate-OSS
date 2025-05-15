package balti.migrate.app.di

import balti.migrate.app.ui.navigation.HomeGraphViewModel
import balti.migrate.app.ui.navigation.RestoreRouteChoicesViewModel
import balti.migrate.app.ui.screens.home.ScreenHomeViewModel
import balti.migrate.app.ui.screens.setupPermission.SetupPermissionScreenViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val appDiModule = module {

    /* ViewModel */

    viewModelOf(::HomeGraphViewModel)
    viewModelOf(::RestoreRouteChoicesViewModel)
    viewModelOf(::SetupPermissionScreenViewModel)
    viewModelOf(::ScreenHomeViewModel)
}