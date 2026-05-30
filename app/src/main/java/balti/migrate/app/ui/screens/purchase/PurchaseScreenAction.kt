package balti.migrate.app.ui.screens.purchase

import android.app.Activity

sealed class PurchaseScreenAction {
    data class OnItemSelected(val item: PurchaseItem, val activity: Activity) : PurchaseScreenAction()
    data object OnRetry : PurchaseScreenAction()
}
