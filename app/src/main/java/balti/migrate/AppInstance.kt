package balti.migrate

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import balti.migrate.utilities.CommonToolKotlin.Companion.FILE_MAIN_PREF


class AppInstance: Application() {

    companion object{
        lateinit var appContext: Context
        lateinit var sharedPrefs: SharedPreferences
    }

    override fun onCreate() {
        super.onCreate()
        appContext = this
        sharedPrefs = getSharedPreferences(FILE_MAIN_PREF, Context.MODE_PRIVATE)
    }

}