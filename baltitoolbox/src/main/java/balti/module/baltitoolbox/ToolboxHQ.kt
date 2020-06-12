package balti.module.baltitoolbox

import android.content.Context
import android.content.SharedPreferences

object ToolboxHQ {

    internal lateinit var context: Context
    internal lateinit var sharedPreferences: SharedPreferences

    fun init(context: Context){
        this.context = context.applicationContext
        sharedPreferences = context.getSharedPreferences(context.packageName, Context.MODE_PRIVATE)
    }

    val FileHandlers by lazy { balti.module.baltitoolbox.functions.FileHandlers }
    val GetResources by lazy { balti.module.baltitoolbox.functions.GetResources }
    val Misc by lazy { balti.module.baltitoolbox.functions.Misc }
    val SharedPrefs by lazy { balti.module.baltitoolbox.functions.SharedPrefs }


}