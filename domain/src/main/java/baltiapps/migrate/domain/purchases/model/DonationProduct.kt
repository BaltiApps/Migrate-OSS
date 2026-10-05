package baltiapps.migrate.domain.purchases.model

data class DonationProduct(
    val tier: DonationTier,
    val name: String,
    val price: String,
) {
    val productId: String
        get() = tier.productId()
}
