package balti.migrate.app.di

import balti.migrate.app.MainActivityViewModel
import balti.migrate.app.data.purchases.PurchasesDataSource
import balti.migrate.app.data.purchases.PurchasesDataSourceImpl
import balti.migrate.app.ui.navigation.HomeGraphViewModel
import balti.migrate.app.ui.navigation.RestoreRouteChoicesViewModel
import balti.migrate.app.ui.screens.appSettings.AppSettingsViewModel
import balti.migrate.app.ui.screens.home.ScreenHomeViewModel
import balti.migrate.app.ui.screens.purchase.PurchaseScreenViewModel
import balti.migrate.app.ui.screens.setupPermission.SetupPermissionScreenViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val appDiModule = module {

    /* Data */

    single<PurchasesDataSource> { PurchasesDataSourceImpl(context = get()) }

    /* ViewModel */

    viewModelOf(::MainActivityViewModel)

    viewModelOf(::HomeGraphViewModel)
    viewModelOf(::RestoreRouteChoicesViewModel)
    viewModelOf(::SetupPermissionScreenViewModel)
    viewModelOf(::ScreenHomeViewModel)
    viewModelOf(::AppSettingsViewModel)
    viewModel { PurchaseScreenViewModel(purchasesDataSource = get()) }
}