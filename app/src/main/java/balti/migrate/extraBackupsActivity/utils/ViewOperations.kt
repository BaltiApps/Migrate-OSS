package balti.migrate.extraBackupsActivity.utils

import android.content.Context
import android.view.View
import android.widget.CheckBox
import android.widget.ProgressBar
import android.widget.TextView

class ViewOperations(val context: Context) {

    fun visibilitySet(view: View, mode: Int){
        try {view.visibility = mode} catch (e: Exception){}
    }

    fun enableSet(view: View, mode: Boolean){
        try {view.isEnabled = mode} catch (e: Exception){}
    }

    fun clickableSet(view: View, mode: Boolean){
        try {view.isClickable = mode} catch (e: Exception){}
    }

    fun checkSet(view: CheckBox, mode: Boolean){
        try {view.isChecked = mode} catch (e: Exception){}
    }

    fun progressSet(view: ProgressBar, p: Int, max: Int = -1){
        try {if (max > 0) view.max = max} catch (e: Exception){}
        try {view.progress = p} catch (e: Exception){}
    }

    fun textSet(view: TextView, text: String){
        try {view.text = text} catch (e: Exception){}
    }

    fun textSet(view: TextView, resId: Int){
        try {view.text = getStringFromRes(resId)} catch (e: Exception){}
    }

    fun getStringFromRes(resId: Int): String{
        return try { context.getString(resId) }
        catch (e: Exception){
            e.printStackTrace()
            ""
        }
    }

    fun doSomething(f:() -> Unit){
        try { f() } catch (e: Exception){}
    }

}