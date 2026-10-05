package balti.migrate.app.ui.screens.purchase

/** One-time events that are shown once and not kept in [PurchaseScreenState]. */
sealed class PurchaseScreenEvent {
    data object PurchaseCancelled : PurchaseScreenEvent()
}
