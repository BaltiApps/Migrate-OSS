package balti.migrate.app.data.purchasesV2

import android.app.Activity
import android.content.Context
import baltiapps.migrate.domain.purchases.mapper.PurchaseMapper
import baltiapps.migrate.domain.purchases.model.DonationProduct
import baltiapps.migrate.domain.purchases.model.DonationPurchaseResult
import baltiapps.migrate.domain.purchases.model.UiContext
import baltiapps.migrate.domain.purchases.sources.GPlayPurchaseDataSource
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import timber.log.Timber
import kotlin.coroutines.resume

class GPlayPurchaseDataSourceImpl (
    private val applicationContext: Context,
    private val mapper: PurchaseMapper<ProductDetails>,
): GPlayPurchaseDataSource(), PurchasesUpdatedListener {

    private val _definedDonationProducts = MutableStateFlow<List<DonationProduct>>(emptyList())
    override val definedDonationProducts: StateFlow<List<DonationProduct>> = _definedDonationProducts.asStateFlow()

    private val _purchasedDonationProductId = MutableStateFlow<String?>(null)
    override val purchasedDonationProductId: StateFlow<String?> = _purchasedDonationProductId.asStateFlow()

    private val _donationPurchaseResult = MutableSharedFlow<DonationPurchaseResult>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    override val donationPurchaseResult: SharedFlow<DonationPurchaseResult> = _donationPurchaseResult.asSharedFlow()

    @Volatile
    private var fetchedProductDetails: List<ProductDetails> = emptyList()

    private val billingClient = BillingClient.newBuilder(applicationContext)
        .setListener(this)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder().enableOneTimeProducts().build()
        )
        .build()

    private val connectionMutex = Mutex()

    override suspend fun ensureConnection() {
        if (billingClient.isReady) return

        connectionMutex.withLock {
            // another caller may have connected while we waited for the lock
            if (billingClient.isReady) return

            val result = suspendCancellableCoroutine { continuation ->
                billingClient.startConnection(object : BillingClientStateListener {
                    override fun onBillingSetupFinished(billingResult: BillingResult) {
                        Timber.d("ensureConnection: billing connected, code=${billingResult.responseCode}")
                        if (continuation.isActive) continuation.resume(billingResult)
                    }
                    override fun onBillingServiceDisconnected() {
                        Timber.w("ensureConnection: billing service disconnected")
                    }
                })
            }

            if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                "ensureConnection: connection failed - ${result.debugMessage}".run {
                    Timber.e(this)
                    throw Exception(this)
                }
            }
        }
    }

    override suspend fun refreshDefinedProducts(): Result<Unit> {
        runCatching { ensureConnection() }.onFailure { return Result.failure(it) }

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(allProductIds.map { id ->
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

        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            fetchedProductDetails = details.productDetailsList.toList()
            _definedDonationProducts.update {
                details.productDetailsList.map {
                    mapper.run {
                        it.toDonationProduct()
                    }
                }
            }
            return Result.success(Unit)
        } else {
            Timber.e("queryProductDetails: failed - ${billingResult.debugMessage}")
            return Result.failure(Exception("queryProductDetails failed: ${billingResult.debugMessage}"))
        }
    }

    override suspend fun refreshPurchase(): Result<Unit> {
        runCatching { ensureConnection() }.onFailure { return Result.failure(it) }

        Timber.d("refreshPurchase: querying existing purchases")

        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()

        val (billingResult, purchases) = suspendCancellableCoroutine { continuation ->
            billingClient.queryPurchasesAsync(params) { billingResult, purchases ->
                continuation.resume(billingResult to purchases)
            }
        }

        if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
            Timber.w("refreshPurchase: query failed - ${billingResult.debugMessage}")
            return Result.failure(Exception("queryPurchases failed: ${billingResult.debugMessage}"))
        }

        Timber.d("refreshPurchase: found ${purchases.size} existing purchase(s)")
        val ownedPurchases = purchases.filter { it.purchaseState == Purchase.PurchaseState.PURCHASED }

        // Safety net: acknowledge anything missed in onPurchasesUpdated, else Google refunds it after 3 days
        ownedPurchases.filter { !it.isAcknowledged }.forEach { acknowledgeInternal(it) }

        val highestOwnedId = getHighestOwnedProductId(ownedPurchases)

        Timber.d("refreshPurchase: owned productId=$highestOwnedId")

        _purchasedDonationProductId.update { highestOwnedId }
        return Result.success(Unit)
    }

    override suspend fun purchaseDonationProduct(
        uiContext: UiContext<*>,
        product: DonationProduct
    ) {
        runCatching { ensureConnection() }.getOrElse {
            _donationPurchaseResult.tryEmit(DonationPurchaseResult.Error)
            return
        }

        Timber.d("purchaseDonationProduct: launching for ${product.productId}")

        if (fetchedProductDetails.isEmpty()) {
            refreshDefinedProducts()
        }

        val productDetailsToBuy = fetchedProductDetails.find { it.productId == product.productId } ?: run {
            Timber.e("purchaseDonationProduct: no ProductDetails found for ${product.productId}")
            _donationPurchaseResult.tryEmit(DonationPurchaseResult.Error)
            return
        }

        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(
                BillingFlowParams.ProductDetailsParams.newBuilder()
                    .setProductDetails(productDetailsToBuy)
                    .build()
            ))
            .build()

        // launchBillingFlow must be called from the main thread
        val result = withContext(Dispatchers.Main) {
            billingClient.launchBillingFlow(uiContext.context as Activity, billingFlowParams)
        }
        Timber.d("purchaseDonationProduct: result code=${result.responseCode} msg=${result.debugMessage}")

        // OK means the sheet is showing, and the outcome arrives in onPurchasesUpdated
        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> Unit
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                _purchasedDonationProductId.update { product.productId }
                _donationPurchaseResult.tryEmit(DonationPurchaseResult.Success(product.productId))
            }
            else -> _donationPurchaseResult.tryEmit(DonationPurchaseResult.Error)
        }
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?) {
        Timber.d("onPurchasesUpdated: code=${billingResult.responseCode} purchases=${purchases?.size}")
        when {
            billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null -> {

                val purchasedProducts = purchases
                    .filter { it.purchaseState == Purchase.PurchaseState.PURCHASED }

                purchasedProducts
                    .filter { !it.isAcknowledged }
                    .forEach { acknowledgeInternal(it) }

                val productId = getHighestOwnedProductId(purchasedProducts) ?: run {
                    if (purchases.any { it.purchaseState == Purchase.PurchaseState.PENDING }) {
                        Timber.d("onPurchasesUpdated: purchase pending")
                        _donationPurchaseResult.tryEmit(DonationPurchaseResult.Pending)
                    } else {
                        Timber.d("onPurchasesUpdated: failed to get purchased product id")
                        _donationPurchaseResult.tryEmit(DonationPurchaseResult.Error)
                    }
                    return
                }

                Timber.d("onPurchasesUpdated: purchased productId=$productId")

                _purchasedDonationProductId.update { productId }
                _donationPurchaseResult.tryEmit(DonationPurchaseResult.Success(productId))
            }
            billingResult.responseCode == BillingClient.BillingResponseCode.USER_CANCELED -> {
                Timber.d("onPurchasesUpdated: user cancelled")
                _donationPurchaseResult.tryEmit(DonationPurchaseResult.Cancelled)
            }
            else -> {
                Timber.w("onPurchasesUpdated: failed - ${billingResult.debugMessage}")
                _donationPurchaseResult.tryEmit(DonationPurchaseResult.Failed)
            }
        }
    }

    private fun acknowledgeInternal(purchase: Purchase) {
        Timber.d("acknowledgeInternal: acknowledging ${purchase.products}")
        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()

        billingClient.acknowledgePurchase(params) { result ->
            Timber.d("acknowledgeInternal: result code=${result.responseCode} msg=${result.debugMessage}")
        }
    }

    private fun getHighestOwnedProductId(purchases: List<Purchase>): String? {
        return purchases
            .flatMap { it.products }
            .filter { it in allProductIds }
            .maxByOrNull { allProductIds.indexOf(it) }
    }
}