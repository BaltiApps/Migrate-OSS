package balti.migrate.app.ui.screens.purchase

import baltiapps.migrate.domain.purchases.model.UiContext

sealed class PurchaseScreenAction {
    data class OnItemSelected(val item: PurchaseItem, val uiContext: UiContext<*>) : PurchaseScreenAction()
    data object OnRetry : PurchaseScreenAction()
    data object OnRestorePurchase : PurchaseScreenAction()
}
