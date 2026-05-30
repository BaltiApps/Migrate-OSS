package balti.migrate.app.ui.screens.purchase

sealed class PurchaseScreenAction {
    data class OnItemSelected(val item: PurchaseItem) : PurchaseScreenAction()
}
