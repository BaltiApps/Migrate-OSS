package baltiapps.migrate.domain.purchases.model

sealed class DonationPurchaseResult {
    data class Success(val productId: String) : DonationPurchaseResult()
    data object Pending : DonationPurchaseResult()
    data object Cancelled : DonationPurchaseResult()
    /** Google Play reported a failure for the purchase. */
    data object Failed : DonationPurchaseResult()
    /** We could not start the purchase, e.g. billing connection failed or product not found. */
    data object Error : DonationPurchaseResult()
}
