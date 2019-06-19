package balti.migrate.extraBackupsActivity

import android.content.Context
import android.support.v7.app.AppCompatActivity
import balti.migrate.utilities.CommonToolKotlin
import balti.migrate.utilities.CommonToolKotlin.Companion.PREF_FILE_MAIN

class ExtraBackupsKotlin : AppCompatActivity(), OnJobCompletion {

    val commonTools by lazy { CommonToolKotlin(this) }
    val main by lazy { getSharedPreferences(PREF_FILE_MAIN, Context.MODE_PRIVATE) }
    val editor by lazy { main.edit() }

    override fun onComplete(jobCode: Int, jobSuccess: Boolean, jobResult: Any?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }


}