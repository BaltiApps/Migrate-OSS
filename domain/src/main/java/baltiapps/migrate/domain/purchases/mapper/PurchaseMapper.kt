package baltiapps.migrate.domain.purchases.mapper

import baltiapps.migrate.domain.purchases.model.DonationProduct

interface PurchaseMapper<T> {
    fun T.toDonationProduct(): DonationProduct
}