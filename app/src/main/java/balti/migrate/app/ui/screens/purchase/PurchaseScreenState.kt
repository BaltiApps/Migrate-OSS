package balti.migrate.app.ui.screens.purchase

data class PurchaseScreenState(
    val items: List<PurchaseItem> = emptyList(),
    val purchaseState: PurchaseState = PurchaseState.Idle,
    val isLoading: Boolean = false,
)

sealed class PurchaseState {
    /** Nothing purchased yet, show the list of items. */
    data object Idle : PurchaseState()
    data object Pending : PurchaseState()
    data class Purchased(val item: PurchaseItem) : PurchaseState()
    data object LoadFailed : PurchaseState()
    data object PurchaseFailed : PurchaseState()
}

data class PurchaseItem(
    val productId: String,
    val title: String,
    val price: String,
)
