package baltiapps.migrate.domain.purchases.usecase

import baltiapps.migrate.domain.purchases.sources.PurchaseStore

class GetSavedPurchaseUseCase(
    private val purchaseStore: PurchaseStore,
) {
    operator fun invoke(): Pair<String, String>? = purchaseStore.getSavedPurchase()
}
