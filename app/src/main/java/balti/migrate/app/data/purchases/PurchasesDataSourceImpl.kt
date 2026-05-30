package balti.migrate.app.data.purchases

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import timber.log.Timber
import kotlin.coroutines.resume

private const val PREFS_NAME = "purchases"
private const val KEY_PURCHASED_PRODUCT_ID = "purchased_product_id"
private const val KEY_PURCHASED_PRODUCT_TITLE = "purchased_product_title"

class PurchasesDataSourceImpl(private val context: Context) : PurchasesDataSource, PurchasesUpdatedListener {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _purchaseUpdates = MutableSharedFlow<MigratePurchaseResult>(extraBufferCapacity = 1)
    override val purchaseUpdates: SharedFlow<MigratePurchaseResult> = _purchaseUpdates.asSharedFlow()

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private var productDetailsMap: Map<String, ProductDetails> = emptyMap()

    private val billingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder().enableOneTimeProducts().build()
        )
        .build()

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?) {
        Timber.d("onPurchasesUpdated: code=${billingResult.responseCode} purchases=${purchases?.size}")
        when {
            billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null -> {
                scope.launch {
                    purchases
                        .filter { it.purchaseState == Purchase.PurchaseState.PURCHASED && !it.isAcknowledged }
                        .forEach { acknowledgeInternal(it) }
                    val productId = purchases
                        .firstOrNull { it.purchaseState == Purchase.PurchaseState.PURCHASED }
                        ?.products?.firstOrNull()
                    Timber.d("onPurchasesUpdated: purchased productId=$productId")
                    _purchaseUpdates.emit(
                        if (productId != null) MigratePurchaseResult.Success(productId) else MigratePurchaseResult.Failed
                    )
                }
            }
            billingResult.responseCode == BillingClient.BillingResponseCode.USER_CANCELED -> {
                Timber.d("onPurchasesUpdated: user cancelled")
                _purchaseUpdates.tryEmit(MigratePurchaseResult.Cancelled)
            }
            else -> {
                Timber.w("onPurchasesUpdated: failed - ${billingResult.debugMessage}")
                _purchaseUpdates.tryEmit(MigratePurchaseResult.Failed)
            }
        }
    }

    override suspend fun fetchProducts(productIds: List<String>): Result<List<MigrateProductInfo>> {
        Timber.d("fetchProducts: connecting to billing for ${productIds.size} products")
        val connectResult = suspendCancellableCoroutine { continuation ->
            billingClient.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(billingResult: BillingResult) {
                    Timber.d("fetchProducts: billing connected, code=${billingResult.responseCode}")
                    continuation.resume(billingResult)
                }
                override fun onBillingServiceDisconnected() {
                    Timber.w("fetchProducts: billing service disconnected")
                }
            })
        }

        if (connectResult.responseCode != BillingClient.BillingResponseCode.OK) {
            Timber.e("fetchProducts: connection failed - ${connectResult.debugMessage}")
            return Result.failure(Exception("Billing connection failed: ${connectResult.debugMessage}"))
        }

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productIds.map { id ->
                QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(id)
                    .setProductType(BillingClient.ProductType.INAPP)
                    .build()
            })
            .build()

        val (billingResult, details) = suspendCancellableCoroutine { continuation ->
            billingClient.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
                continuation.resume(billingResult to productDetailsList)
            }
        }

        if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
            Timber.e("fetchProducts: product query failed - ${billingResult.debugMessage}")
            return Result.failure(Exception("Product query failed: ${billingResult.debugMessage}"))
        }

        val detailsList = details.productDetailsList
        Timber.d("fetchProducts: received ${detailsList.size} products: ${detailsList.map { it.productId }}")
        productDetailsMap = detailsList.associateBy { it.productId }
        return Result.success(detailsList.map { it.toMigrateProductInfo() })
    }

    override suspend fun getOwnedProduct(productIds: List<String>): MigrateProductInfo? {
        Timber.d("getOwnedProduct: querying existing purchases")
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()

        val (billingResult, purchases) = suspendCancellableCoroutine { continuation ->
            billingClient.queryPurchasesAsync(params) { billingResult, purchases ->
                continuation.resume(billingResult to purchases)
            }
        }

        if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
            Timber.w("getOwnedProduct: query failed - ${billingResult.debugMessage}")
            return null
        }

        Timber.d("getOwnedProduct: found ${purchases.size} existing purchase(s)")
        val ownedId = purchases
            .firstOrNull { it.purchaseState == Purchase.PurchaseState.PURCHASED }
            ?.products
            ?.firstOrNull { it in productIds }

        Timber.d("getOwnedProduct: owned productId=$ownedId")
        return ownedId?.let { productDetailsMap[it]?.toMigrateProductInfo() }
    }

    override suspend fun restorePurchases(productIds: List<String>): MigrateProductInfo? {
        Timber.d("restorePurchases: triggered by user")
        return getOwnedProduct(productIds)
    }

    override fun launchBillingFlow(activity: Activity, productId: String) {
        val productDetails = productDetailsMap[productId] ?: run {
            Timber.e("launchBillingFlow: no ProductDetails found for $productId")
            return
        }
        Timber.d("launchBillingFlow: launching for $productId")
        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(
                BillingFlowParams.ProductDetailsParams.newBuilder()
                    .setProductDetails(productDetails)
                    .build()
            ))
            .build()
        val result = billingClient.launchBillingFlow(activity, billingFlowParams)
        Timber.d("launchBillingFlow: result code=${result.responseCode} msg=${result.debugMessage}")
    }

    override fun saveLocalPurchase(productId: String, title: String) {
        Timber.d("saveLocalPurchase: productId=$productId title=$title")
        prefs.edit()
            .putString(KEY_PURCHASED_PRODUCT_ID, productId)
            .putString(KEY_PURCHASED_PRODUCT_TITLE, title)
            .apply()
    }

    override fun getLocalPurchase(): Pair<String, String>? {
        val productId = prefs.getString(KEY_PURCHASED_PRODUCT_ID, null) ?: return null
        val title = prefs.getString(KEY_PURCHASED_PRODUCT_TITLE, null) ?: ""
        Timber.d("getLocalPurchase: productId=$productId title=$title")
        return productId to title
    }

    private suspend fun acknowledgeInternal(purchase: Purchase) {
        Timber.d("acknowledgeInternal: acknowledging ${purchase.products}")
        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()
        val result = suspendCancellableCoroutine { continuation ->
            billingClient.acknowledgePurchase(params) { result ->
                continuation.resume(result)
            }
        }
        Timber.d("acknowledgeInternal: result code=${result.responseCode} msg=${result.debugMessage}")
    }

    private fun ProductDetails.toMigrateProductInfo() = MigrateProductInfo(
        productId = productId,
        title = name,
        price = oneTimePurchaseOfferDetails?.formattedPrice ?: "",
    )
}
