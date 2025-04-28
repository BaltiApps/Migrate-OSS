package balti.migrate.app.di

import balti.migrate.app.ui.navigation.RestoreRouteChoicesViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val appDiModule = module {

    /* ViewModel */

    viewModelOf(::RestoreRouteChoicesViewModel)
}