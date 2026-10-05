package balti.migrate.app.ui.screens.purchase

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import baltiapps.migrate.domain.purchases.model.DonationProduct
import baltiapps.migrate.domain.purchases.model.DonationPurchaseResult
import baltiapps.migrate.domain.purchases.usecase.BuyAnItemUseCase
import baltiapps.migrate.domain.purchases.usecase.GetSavedPurchaseUseCase
import baltiapps.migrate.domain.purchases.usecase.ObservePurchaseStateUseCase
import baltiapps.migrate.domain.purchases.usecase.RefreshPurchaseStateUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PurchaseScreenViewModel(
    private val observePurchaseStateUseCase: ObservePurchaseStateUseCase,
    private val refreshPurchaseStateUseCase: RefreshPurchaseStateUseCase,
    private val buyAnItemUseCase: BuyAnItemUseCase,
    private val getSavedPurchaseUseCase: GetSavedPurchaseUseCase,
) : ViewModel() {

    private var products: List<DonationProduct> = emptyList()
    private var purchasedProductId: String? = null

    private val _state = MutableStateFlow(PurchaseScreenState())
    val state = _state.asStateFlow()

    init {
        _state.update { it.copy(purchasedItem = getSavedPurchaseItem()) }
        viewModelScope.launch {
            observePurchaseStateUseCase.invoke(
                onProductsChanged = {
                    products = it
                    renderItems()
                },
                onPurchasedProductChanged = {
                    purchasedProductId = it
                    renderItems()
                },
            )
        }
        refresh()
    }

    fun performAction(action: PurchaseScreenAction) {
        when (action) {
            is PurchaseScreenAction.OnItemSelected -> buy(action)
            is PurchaseScreenAction.OnRetry -> refresh()
            is PurchaseScreenAction.OnRestorePurchase -> refresh()
        }
    }

    private fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, isError = false) }
            refreshPurchaseStateUseCase.invoke()
            renderItems()
            _state.update {
                it.copy(
                    isLoading = false,
                    isError = it.items.isEmpty() && it.purchasedItem == null,
                )
            }
        }
    }

    private fun buy(action: PurchaseScreenAction.OnItemSelected) {
        val product = products.firstOrNull { it.productId == action.item.productId } ?: return
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, isPending = false) }
            val result = buyAnItemUseCase.invoke(product, action.uiContext)
            // the new owned tier arrives through the observed flows
            renderItems()
            _state.update {
                it.copy(
                    isLoading = false,
                    isPending = result is DonationPurchaseResult.Pending,
                )
            }
        }
    }

    private fun renderItems() {
        val items = products
            .sortedBy { it.tier.tier }
            .map { it.toPurchaseItem() }
        val purchasedItem = items.firstOrNull { it.productId == purchasedProductId }
            ?: getSavedPurchaseItem()
        _state.update { it.copy(items = items, purchasedItem = purchasedItem) }
    }

    private fun getSavedPurchaseItem(): PurchaseItem? {
        return getSavedPurchaseUseCase.invoke()?.let { (id, name) -> PurchaseItem(id, name, "") }
    }

    private fun DonationProduct.toPurchaseItem() = PurchaseItem(
        productId = productId,
        title = name,
        price = price,
    )
}
