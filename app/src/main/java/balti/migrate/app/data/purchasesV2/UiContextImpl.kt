package balti.migrate.app.data.purchasesV2

import android.app.Activity
import baltiapps.migrate.domain.purchases.model.UiContext

data class UiContextImpl(
    override val context: Activity
): UiContext<Activity>
