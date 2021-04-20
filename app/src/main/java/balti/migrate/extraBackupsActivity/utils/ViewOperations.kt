package balti.migrate.extraBackupsActivity.utils

import android.content.Context
import android.view.View
import android.widget.CheckBox
import android.widget.ProgressBar
import android.widget.TextView
import balti.module.baltitoolbox.functions.Misc.tryIt

class ViewOperations(val context: Context) {

    fun visibilitySet(view: View, mode: Int){
        tryIt {view.visibility = mode}
    }

    fun enableSet(view: View, mode: Boolean){
        tryIt {view.isEnabled = mode}
    }

    fun clickableSet(view: View, mode: Boolean){
        tryIt {view.isClickable = mode}
    }

    fun checkSet(view: CheckBox, mode: Boolean){
        tryIt {view.isChecked = mode}
    }

    fun progressSet(view: ProgressBar, p: Int, max: Int = -1){
        tryIt {if (max > 0) view.max = max}
        tryIt {view.progress = p}
    }

    fun textSet(view: TextView, text: String){
        tryIt {view.text = text}
    }

    fun textSet(view: TextView, resId: Int){
        tryIt {view.text = getStringFromRes(resId)}
    }

    fun getStringFromRes(resId: Int): String{
        return try { context.getString(resId) }
        catch (e: Exception){
            e.printStackTrace()
            ""
        }
    }

    fun doSomething(f:() -> Unit){
        tryIt { f() }
    }

}