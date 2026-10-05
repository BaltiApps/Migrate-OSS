package balti.migrate.app.data.purchasesV2

import android.content.Context
import androidx.core.content.edit
import baltiapps.migrate.domain.purchases.sources.PurchaseStore
import timber.log.Timber

class PurchaseStoreImpl(
    private val applicationContext: Context
): PurchaseStore {

    companion object {
        private const val PREFS_NAME = "purchases"
        private const val KEY_PURCHASED_PRODUCT_ID = "purchased_product_id"
        private const val KEY_PURCHASED_PRODUCT_NAME = "purchased_product_title"
    }

    private val prefs = applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun savePurchase(productId: String, name: String) {
        Timber.d("savePurchase: productId=$productId name=$name")
        prefs.edit {
            putString(KEY_PURCHASED_PRODUCT_ID, productId)
                .putString(KEY_PURCHASED_PRODUCT_NAME, name)
        }
    }

    override fun getSavedPurchase(): Pair<String, String>? {
        val productId = prefs.getString(KEY_PURCHASED_PRODUCT_ID, null) ?: return null
        val name = prefs.getString(KEY_PURCHASED_PRODUCT_NAME, null) ?: ""
        Timber.d("getSavedPurchase: productId=$productId name=$name")
        return productId to name
    }

    override fun clearPurchase() {
        prefs.edit {
            remove(KEY_PURCHASED_PRODUCT_ID)
            remove(KEY_PURCHASED_PRODUCT_NAME)
        }
    }

}