package balti.module.baltitoolbox

import android.content.Context
import android.content.SharedPreferences

object ToolboxHQ {

    internal lateinit var context: Context
    internal lateinit var sharedPreferences: SharedPreferences

    fun init(context: Context){
        this.context = context
        sharedPreferences = context.getSharedPreferences(context.packageName, Context.MODE_PRIVATE)
    }

    val FileHandlers = balti.module.baltitoolbox.functions.FileHandlers
    val GetResources = balti.module.baltitoolbox.functions.GetResources
    val Misc = balti.module.baltitoolbox.functions.Misc
    val SharedPrefs = balti.module.baltitoolbox.functions.SharedPrefs

    val AsyncCoroutineTask = balti.module.baltitoolbox.jobHandlers.AsyncCoroutineTask

}