package baltiapps.migrate.domain.purchases.model

enum class DonationTier(val tier: Int) {
    SUPPORTER(1),
    BACKER(2),
    LEGEND(3),
    ;

    fun productId(): String {
        return "migrate_donate_${this.name.lowercase()}"
    }

    companion object {
        fun getTier(productId: String): DonationTier {
            return DonationTier.entries.first { it.productId() == productId }
        }
    }
}