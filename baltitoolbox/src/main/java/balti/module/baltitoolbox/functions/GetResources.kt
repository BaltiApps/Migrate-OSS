package balti.module.baltitoolbox.functions

import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.view.View
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.widget.ImageViewCompat
import balti.module.baltitoolbox.ToolboxHQ

object GetResources {

    private val context = ToolboxHQ.context

    fun getStringFromRes(id: Int): String = context.getString(id)
    fun getDrawableFromRes(id: Int): Drawable? = context.getDrawable(id)
    fun getColorFromRes(id: Int): Int = ContextCompat.getColor(context, id)

    fun setTintFromRes(view: View, rColor: Int) {
        ViewCompat.setBackgroundTintList(view, ColorStateList.valueOf(getColorFromRes(rColor)))
    }
    fun setTintFromRes(image: ImageView, rColor: Int) {
        ImageViewCompat.setImageTintList(image, ColorStateList.valueOf(getColorFromRes(rColor)))
    }

}