package baltiapps.migrate.domain.purchases.usecase

import baltiapps.migrate.domain.purchases.sources.GPlayPurchaseDataSource
import baltiapps.migrate.domain.purchases.sources.PurchaseStore

class RefreshPurchaseStateUseCase(
    private val purchaseDataSource: GPlayPurchaseDataSource,
    private val purchaseStore: PurchaseStore,
) {
    suspend operator fun invoke() {
        val productsFetchResult = runCatching { purchaseDataSource.refreshDefinedProducts() }.getOrElse {
            Result.failure(it)
        }
        val purchaseFetchResult = runCatching { purchaseDataSource.refreshPurchase() }.getOrElse {
            Result.failure(it)
        }

        if (productsFetchResult.isFailure || purchaseFetchResult.isFailure) {
            return
        }

        val products = purchaseDataSource.definedDonationProducts.value
        val purchasedId = purchaseDataSource.purchasedDonationProductId.value

        when {
            products.isNotEmpty() && purchasedId != null -> {
                products.find { it.productId == purchasedId }?.run {
                    purchaseStore.savePurchase(this.productId, this.name)
                }
            }
            products.isNotEmpty() -> {
                // no purchase id may mean a refund, so clear it
                purchaseStore.clearPurchase()
            }
        }
    }
}