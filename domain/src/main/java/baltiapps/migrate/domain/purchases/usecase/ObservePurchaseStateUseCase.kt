package baltiapps.migrate.domain.purchases.usecase

import baltiapps.migrate.domain.purchases.model.DonationProduct
import baltiapps.migrate.domain.purchases.sources.GPlayPurchaseDataSource
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

class ObservePurchaseStateUseCase(
    private val purchaseDataSource: GPlayPurchaseDataSource,
) {
    /**
     * Suspends and delivers updates until the calling coroutine is cancelled.
     */
    suspend operator fun invoke(
        onProductsChanged: (List<DonationProduct>) -> Unit,
        onPurchasedProductChanged: (String?) -> Unit,
    ) = coroutineScope {
        launch {
            purchaseDataSource.definedDonationProducts.collect { onProductsChanged(it) }
        }
        launch {
            purchaseDataSource.purchasedDonationProductId.collect { onPurchasedProductChanged(it) }
        }
    }
}
