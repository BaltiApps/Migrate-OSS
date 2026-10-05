package balti.migrate.app.di

import balti.migrate.app.MainActivityViewModel
import balti.migrate.app.data.purchasesV2.GPlayPurchaseDataSourceImpl
import balti.migrate.app.data.purchasesV2.PurchaseMapperImpl
import balti.migrate.app.data.purchasesV2.PurchaseStoreImpl
import balti.migrate.app.ui.navigation.HomeGraphViewModel
import balti.migrate.app.ui.navigation.RestoreRouteChoicesViewModel
import balti.migrate.app.ui.screens.appSettings.AppSettingsViewModel
import balti.migrate.app.ui.screens.home.ScreenHomeViewModel
import balti.migrate.app.ui.screens.purchase.PurchaseScreenViewModel
import balti.migrate.app.ui.screens.setupPermission.SetupPermissionScreenViewModel
import baltiapps.migrate.domain.purchases.mapper.PurchaseMapper
import baltiapps.migrate.domain.purchases.sources.GPlayPurchaseDataSource
import baltiapps.migrate.domain.purchases.sources.PurchaseStore
import baltiapps.migrate.domain.purchases.usecase.BuyAnItemUseCase
import baltiapps.migrate.domain.purchases.usecase.GetSavedPurchaseUseCase
import baltiapps.migrate.domain.purchases.usecase.ObservePurchaseStateUseCase
import baltiapps.migrate.domain.purchases.usecase.RefreshPurchaseStateUseCase
import com.android.billingclient.api.ProductDetails
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.bind
import org.koin.dsl.module

val appDiModule = module {

    /* Data */

    single<PurchaseMapper<ProductDetails>> { PurchaseMapperImpl() }
    singleOf(::GPlayPurchaseDataSourceImpl) bind GPlayPurchaseDataSource::class
    singleOf(::PurchaseStoreImpl) bind PurchaseStore::class

    /* Use cases */

    singleOf(::ObservePurchaseStateUseCase)
    singleOf(::RefreshPurchaseStateUseCase)
    singleOf(::BuyAnItemUseCase)
    singleOf(::GetSavedPurchaseUseCase)

    /* ViewModel */

    viewModelOf(::MainActivityViewModel)

    viewModelOf(::HomeGraphViewModel)
    viewModelOf(::RestoreRouteChoicesViewModel)
    viewModelOf(::SetupPermissionScreenViewModel)
    viewModelOf(::ScreenHomeViewModel)
    viewModelOf(::AppSettingsViewModel)
    viewModelOf(::PurchaseScreenViewModel)
}