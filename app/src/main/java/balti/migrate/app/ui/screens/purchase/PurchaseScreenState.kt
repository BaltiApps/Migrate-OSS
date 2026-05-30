package balti.migrate.app.ui.screens.purchase

data class PurchaseScreenState(
    val items: List<PurchaseItem> = emptyList(),
    val purchasedItem: PurchaseItem? = null,
    val isLoading: Boolean = false,
)

data class PurchaseItem(
    val title: String,
    val price: String,
)
