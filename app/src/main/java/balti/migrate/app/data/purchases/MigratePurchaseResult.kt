package balti.migrate.app.data.purchases

sealed class MigratePurchaseResult {
    data class Success(val productId: String) : MigratePurchaseResult()
    data object Cancelled : MigratePurchaseResult()
    data object Failed : MigratePurchaseResult()
}
