package balti.migrate.app.ui.screens.purchase

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import balti.migrate.app.data.purchases.MigrateProductInfo
import balti.migrate.app.data.purchases.MigratePurchaseResult
import balti.migrate.app.data.purchases.PurchasesDataSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PurchaseScreenViewModel(
    private val purchasesDataSource: PurchasesDataSource,
) : ViewModel() {

    private val productIds = listOf(
        "migrate_donate_supporter",
        "migrate_donate_backer",
        "migrate_donate_legend",
    )

    private val _state = MutableStateFlow(PurchaseScreenState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            purchasesDataSource.purchaseUpdates.collect { result ->
                handlePurchaseResult(result)
            }
        }
        loadProducts()
    }

    fun performAction(action: PurchaseScreenAction) {
        when (action) {
            is PurchaseScreenAction.OnItemSelected -> {
                _state.update { it.copy(isLoading = true) }
                purchasesDataSource.launchBillingFlow(action.activity, action.item.productId)
            }
            is PurchaseScreenAction.OnRetry -> loadProducts()
            is PurchaseScreenAction.OnRestorePurchase -> restorePurchases()
        }
    }

    private fun restorePurchases() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val info = purchasesDataSource.restorePurchases(productIds)
            if (info != null) {
                purchasesDataSource.saveLocalPurchase(info.productId, info.title)
                val purchasedItem = _state.value.items.firstOrNull { it.productId == info.productId }
                _state.update { it.copy(purchasedItem = purchasedItem, isLoading = false) }
            } else {
                _state.update { it.copy(isLoading = false) }
            }
        }
    }

    private fun loadProducts() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, isError = false) }

            val localPurchase = purchasesDataSource.getLocalPurchase()?.let { (id, title) -> PurchaseItem(id, title, "") }
            val productsResult = purchasesDataSource.fetchProducts(productIds)

            if (productsResult.isFailure) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        isError = localPurchase == null,
                        purchasedItem = localPurchase
                    )
                }
                return@launch
            }

            val items = productIds.mapNotNull { id ->
                productsResult.getOrThrow().firstOrNull { it.productId == id }?.toPurchaseItem()
            }

            val ownedProduct = purchasesDataSource.getOwnedProduct(productIds)
            val purchasedItem = if (ownedProduct != null) {
                purchasesDataSource.saveLocalPurchase(ownedProduct.productId, ownedProduct.title)
                items.firstOrNull { it.productId == ownedProduct.productId }
            } else {
                localPurchase
            }

            _state.update { it.copy(items = items, purchasedItem = purchasedItem, isLoading = false) }
        }
    }

    private fun handlePurchaseResult(result: MigratePurchaseResult) {
        when (result) {
            is MigratePurchaseResult.Success -> {
                val purchasedItem = _state.value.items.firstOrNull { it.productId == result.productId }
                purchasesDataSource.saveLocalPurchase(result.productId, purchasedItem?.title ?: "")
                _state.update { it.copy(purchasedItem = purchasedItem, isLoading = false) }
            }
            is MigratePurchaseResult.Cancelled, is MigratePurchaseResult.Failed ->
                _state.update { it.copy(isLoading = false) }
        }
    }

    private fun MigrateProductInfo.toPurchaseItem() = PurchaseItem(
        productId = productId,
        title = title,
        price = price,
    )
}
