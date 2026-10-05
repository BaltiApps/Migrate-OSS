package baltiapps.migrate.domain.purchases.sources

import baltiapps.migrate.domain.purchases.model.DonationProduct
import baltiapps.migrate.domain.purchases.model.DonationPurchaseResult
import baltiapps.migrate.domain.purchases.model.DonationTier
import baltiapps.migrate.domain.purchases.model.UiContext
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

abstract class GPlayPurchaseDataSource {
    abstract val definedDonationProducts: StateFlow<List<DonationProduct>>
    abstract val purchasedDonationProductId: StateFlow<String?>

    abstract val donationPurchaseResult: SharedFlow<DonationPurchaseResult>

    val allProductIds: List<String>
        get() = DonationTier.entries.map { it.productId() }

    protected abstract suspend fun ensureConnection()
    abstract suspend fun refreshDefinedProducts(): Result<Unit>
    abstract suspend fun refreshPurchase(): Result<Unit>
    abstract suspend fun purchaseDonationProduct(uiContext: UiContext<*>, product: DonationProduct)
}