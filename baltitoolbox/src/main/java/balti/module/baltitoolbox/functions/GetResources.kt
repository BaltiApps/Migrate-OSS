package balti.module.baltitoolbox.functions

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.util.TypedValue
import android.view.View
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.widget.ImageViewCompat
import balti.module.baltitoolbox.ToolboxHQ

object GetResources {

    private val context = ToolboxHQ.context

    fun getStringFromRes(id: Int): String = if (id == 0) "" else context.getString(id)
    fun getDrawableFromRes(id: Int): Drawable? = context.getDrawable(id)
    fun getColorFromRes(id: Int): Int = ContextCompat.getColor(context, id)

    fun getResourceFromAttr(id: Int, context: Context? = null): Int? {
        val a =  TypedValue()
        return try {
            (context ?: this.context).theme.resolveAttribute(id, a, true)
            a.data
        } catch (_: Exception){
            null
        }
    }

    fun setTintFromRes(view: View, rColor: Int, rawColor: Boolean = false) {
        ViewCompat.setBackgroundTintList(view, ColorStateList.valueOf(
            if (!rawColor) getColorFromRes(rColor) else rColor
        ))
    }
    fun setTintFromRes(image: ImageView, rColor: Int, rawColor: Boolean = false) {
        ImageViewCompat.setImageTintList(image, ColorStateList.valueOf(
            if (!rawColor) getColorFromRes(rColor) else rColor
        ))
    }

}