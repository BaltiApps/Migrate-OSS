package balti.migrate.app.data.purchases

import android.app.Activity
import kotlinx.coroutines.flow.SharedFlow

interface PurchasesDataSource {
    val purchaseUpdates: SharedFlow<MigratePurchaseResult>
    suspend fun fetchProducts(productIds: List<String>): Result<List<MigrateProductInfo>>
    suspend fun getOwnedProduct(productIds: List<String>): MigrateProductInfo?
    suspend fun restorePurchases(productIds: List<String>): MigrateProductInfo?
    fun launchBillingFlow(activity: Activity, productId: String)
    fun saveLocalPurchase(productId: String, title: String)
    fun getLocalPurchase(): Pair<String, String>?
}
