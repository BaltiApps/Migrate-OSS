package balti.migrate.common.data.model

import android.graphics.Color
import android.graphics.drawable.Drawable
import androidx.core.graphics.drawable.toDrawable
import baltiapps.migrate.domain.common.model.DrawableAsset

data class AppIcon(
    val drawable: Drawable = Color.TRANSPARENT.toDrawable(),
): DrawableAsset