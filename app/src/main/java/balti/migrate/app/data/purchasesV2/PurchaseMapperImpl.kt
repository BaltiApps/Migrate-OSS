package balti.migrate.app.data.purchasesV2

import baltiapps.migrate.domain.purchases.mapper.PurchaseMapper
import baltiapps.migrate.domain.purchases.model.DonationProduct
import baltiapps.migrate.domain.purchases.model.DonationTier
import com.android.billingclient.api.ProductDetails

class PurchaseMapperImpl: PurchaseMapper<ProductDetails> {
    override fun ProductDetails.toDonationProduct(): DonationProduct {
        val tier = DonationTier.getTier(this.productId)
        return DonationProduct(
            tier = tier,
            name = this.name,
            price = oneTimePurchaseOfferDetails?.formattedPrice ?: "",
        )
    }
}