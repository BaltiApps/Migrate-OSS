package baltiapps.migrate.domain.purchases.sources

interface PurchaseStore {
    fun savePurchase(productId: String, name: String)
    fun getSavedPurchase(): Pair<String, String>?
    fun clearPurchase()
}