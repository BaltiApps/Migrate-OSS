package balti.migrate.utilities

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.os.AsyncTask
import android.os.AsyncTask.THREAD_POOL_EXECUTOR
import android.widget.ImageView
import balti.migrate.R
import balti.migrate.backupActivity.containers.BackupDataPacketKotlin
import java.io.BufferedReader
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileReader

class IconTools {

    fun getIconString(packageInfo: PackageInfo, pm: PackageManager): String {

        val stream = ByteArrayOutputStream()

        val drawable = pm.getApplicationIcon(packageInfo.applicationInfo)
        val icon = Bitmap.createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(icon)

        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)

        icon.compress(Bitmap.CompressFormat.PNG, 100, stream)

        val bytes = stream.toByteArray()
        var res = ""
        for (b in bytes){
            res = res + b + "_"
        }
        return res
    }

    fun loadIconFromApplication(iconView: ImageView, dp: BackupDataPacketKotlin, pm: PackageManager){

        class Loader : AsyncTask<Any, Any, Drawable>(){
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
                try {result?.let { iconView.setImageDrawable(it) }} catch (e: Exception) {e.printStackTrace()}
            }
        }

        Loader().executeOnExecutor(THREAD_POOL_EXECUTOR)
    }

    fun setIconFromIconString(iconView: ImageView, iconString: String){

        class Setter : AsyncTask<Any, Any, Bitmap?>(){
            override fun doInBackground(vararg params: Any?): Bitmap? {

                return try {

                    val byteChunks = iconString.trim().split("_")

                    val filterData = ArrayList<Byte>(0)
                    for (byte in byteChunks) {
                        try {
                            filterData.add(java.lang.Byte.parseByte(byte))
                        }
                        catch (e: Exception){}
                    }

                    val imageData = ByteArray(filterData.size)
                    for (d in imageData.indices) imageData[d] = filterData[d]

                    BitmapFactory.decodeByteArray(imageData, 0, imageData.size)

                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
            }

            override fun onPostExecute(result: Bitmap?) {
                super.onPostExecute(result)
                if (result != null) {
                    iconView.setImageBitmap(result)
                } else {
                    iconView.setImageResource(R.drawable.ic_save_icon)
                }
            }
        }
        Setter().execute()
    }

    fun setIconFromFile(iconView: ImageView, file: File){
        var icon = ""
        BufferedReader(FileReader(file)).readLines().forEach {
            icon += it
        }

        setIconFromIconString(iconView, icon)
    }
}