package baltiapps.migrate.domain.purchases.usecase

import baltiapps.migrate.domain.purchases.model.DonationProduct
import baltiapps.migrate.domain.purchases.model.DonationPurchaseResult
import baltiapps.migrate.domain.purchases.model.UiContext
import baltiapps.migrate.domain.purchases.sources.GPlayPurchaseDataSource
import baltiapps.migrate.domain.purchases.sources.PurchaseStore
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first

class BuyAnItemUseCase(
    private val purchaseDataSource: GPlayPurchaseDataSource,
    private val purchaseStore: PurchaseStore,
) {
    suspend operator fun invoke(
        donationProduct: DonationProduct,
        uiContext: UiContext<*>
    ): DonationPurchaseResult {
        // Subscribe before launching, as SharedFlow drops emissions with no subscriber
        val purchaseResult = coroutineScope {
            val deferred = async(start = CoroutineStart.UNDISPATCHED) {
                purchaseDataSource.donationPurchaseResult.first()
            }
            purchaseDataSource.purchaseDonationProduct(
                uiContext = uiContext,
                product = donationProduct,
            )
            deferred.await()
        }
        return purchaseResult.also { result ->
            if (result is DonationPurchaseResult.Success) {
                val item = purchaseDataSource.definedDonationProducts.value
                    .find { it.productId == result.productId }
                    ?: run {
                        println("BuyAnItemUseCase: purchased ${result.productId} not in defined products, not saved")
                        return@also
                    }
                purchaseStore.savePurchase(item.productId, item.name)
            }
        }
    }
}