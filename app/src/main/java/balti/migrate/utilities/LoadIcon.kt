package balti.migrate.utilities

import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.AsyncTask
import android.widget.ImageView
import balti.migrate.backupActivity.BackupDataPacketKotlin

class LoadIcon(val icon: ImageView, val dp: BackupDataPacketKotlin, val pm: PackageManager) : AsyncTask<Any, Any, Drawable>(){
    override fun doInBackground(vararg params: Any?): Drawable? {
        return try {
            pm.getApplicationIcon(dp.PACKAGE_INFO.applicationInfo)
        }
        catch (e: Exception){
            e.printStackTrace()
            null
        }
    }

    override fun onPostExecute(result: Drawable?) {
        super.onPostExecute(result)
        try {result?.let { icon.setImageDrawable(it) }} catch (e: Exception) {e.printStackTrace()}
    }
}