package balti.migrate.app.ui.screens.purchase

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import baltiapps.migrate.domain.purchases.model.DonationProduct
import baltiapps.migrate.domain.purchases.model.DonationPurchaseResult
import baltiapps.migrate.domain.purchases.usecase.BuyAnItemUseCase
import baltiapps.migrate.domain.purchases.usecase.GetSavedPurchaseUseCase
import baltiapps.migrate.domain.purchases.usecase.ObservePurchaseStateUseCase
import baltiapps.migrate.domain.purchases.usecase.RefreshPurchaseStateUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

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

    private val _events = Channel<PurchaseScreenEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        _state.update {
            it.copy(purchaseState = getSavedPurchaseItem()?.let { item -> PurchaseState.Purchased(item) } ?: PurchaseState.Idle)
        }
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
            is PurchaseScreenAction.OnRefresh -> refresh(minDurationMillis = MANUAL_REFRESH_MIN_DURATION_MILLIS)
            is PurchaseScreenAction.OnRestorePurchase -> refresh()
        }
    }

    private fun refresh(minDurationMillis: Long = 0) {
        if (_state.value.isLoading) return
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, purchaseState = it.purchaseState.withoutNotices()) }
            // keep the loading bar visible for at least this long, the refresh itself can finish instantly
            val minDuration = launch { delay(minDurationMillis.milliseconds) }
            refreshPurchaseStateUseCase.invoke()
            minDuration.join()
            renderItems()
            _state.update {
                it.copy(
                    isLoading = false,
                    purchaseState = if (it.purchaseState is PurchaseState.Idle && it.items.isEmpty()) {
                        PurchaseState.LoadFailed
                    } else {
                        it.purchaseState
                    },
                )
            }
        }
    }

    private fun buy(action: PurchaseScreenAction.OnItemSelected) {
        val product = products.firstOrNull { it.productId == action.item.productId } ?: return
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, purchaseState = PurchaseState.Idle) }
            val result = buyAnItemUseCase.invoke(product, action.uiContext)
            // the new owned tier arrives through the observed flows
            renderItems()
            _state.update {
                val purchaseState = when (result) {
                    is DonationPurchaseResult.Pending -> PurchaseState.Pending
                    is DonationPurchaseResult.Cancelled -> {
                        _events.trySend(PurchaseScreenEvent.PurchaseCancelled)
                        it.purchaseState
                    }
                    is DonationPurchaseResult.Failed, is DonationPurchaseResult.Error -> PurchaseState.PurchaseFailed
                    is DonationPurchaseResult.Success -> it.purchaseState
                }
                it.copy(isLoading = false, purchaseState = purchaseState)
            }
        }
    }

    private fun renderItems() {
        val items = products
            .sortedBy { it.tier.tier }
            .map { it.toPurchaseItem() }
        val purchasedItem = items.firstOrNull { it.productId == purchasedProductId }
            ?: getSavedPurchaseItem()
        _state.update {
            val purchaseState = when {
                purchasedItem != null -> PurchaseState.Purchased(purchasedItem)
                it.purchaseState is PurchaseState.Purchased -> PurchaseState.Idle   // e.g. refunded
                else -> it.purchaseState
            }
            it.copy(items = items, purchaseState = purchaseState)
        }
    }

    private companion object {
        const val MANUAL_REFRESH_MIN_DURATION_MILLIS = 500L
    }

    private fun PurchaseState.withoutNotices(): PurchaseState = when (this) {
        is PurchaseState.LoadFailed, is PurchaseState.PurchaseFailed -> PurchaseState.Idle
        else -> this
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
