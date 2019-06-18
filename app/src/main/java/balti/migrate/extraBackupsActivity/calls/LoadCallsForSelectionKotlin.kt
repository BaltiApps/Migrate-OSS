package balti.migrate.extraBackupsActivity.calls

import android.content.Context
import android.os.AsyncTask

class LoadCallsForSelectionKotlin(private val jobCode: Int, val context: Context,
                                  private val itemList: ArrayList<CallsDataPacketsKotlin> = ArrayList(0)):
        AsyncTask<Any, Any, Any>() {
    override fun doInBackground(vararg params: Any?): Any {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }


}