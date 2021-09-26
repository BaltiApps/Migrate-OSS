package balti.migrate.utilities

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.widget.ImageView
import balti.filex.FileX
import balti.migrate.AppInstance
import balti.migrate.R
import balti.migrate.backupActivity.containers.BackupDataPacketKotlin
import balti.module.baltitoolbox.functions.Misc.doBackgroundTask
import balti.module.baltitoolbox.functions.Misc.tryIt
import java.io.ByteArrayOutputStream

class IconTools {

    private val commonTools by lazy { CommonToolsKotlin(AppInstance.appContext) }

    fun getBitmap(packageInfo: PackageInfo, pm: PackageManager): Bitmap {
        val drawable = pm.getApplicationIcon(packageInfo.applicationInfo)
        val icon = Bitmap.createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(icon)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return icon
    }

    fun getIconString(packageInfo: PackageInfo, pm: PackageManager): String {

        val stream = ByteArrayOutputStream()

        getBitmap(packageInfo, pm).compress(Bitmap.CompressFormat.PNG, 80, stream)

        val bytes = stream.toByteArray()
        val res = StringBuilder()
        for (b in bytes){
            res.append(b).append("_")
        }
        return res.toString()
    }

    fun loadIconFromApplication(iconView: ImageView, dp: BackupDataPacketKotlin, pm: PackageManager){

        doBackgroundTask({
            return@doBackgroundTask try {
                pm.getApplicationIcon(dp.PACKAGE_INFO.applicationInfo)
            }
            catch (e: Exception){
                e.printStackTrace()
                null
            }
        }, {
            tryIt { if (it is Drawable) iconView.setImageDrawable(it) }
        })
    }

    fun loadIconFromApplication(iconView: ImageView, packageName: String, pm: PackageManager){
        doBackgroundTask({
            return@doBackgroundTask try {
                pm.getApplicationIcon(packageName)
            }
            catch (e: Exception){
                e.printStackTrace()
                null
            }
        }, {
            tryIt { if (it is Drawable) iconView.setImageDrawable(it) }
        })
    }

    fun setIconFromIconString(iconView: ImageView, iconString: String){

        doBackgroundTask({
            return@doBackgroundTask try {

                val byteChunks = iconString.trim().split("_")

                val filterData = ArrayList<Byte>(0)
                for (byte in byteChunks) {
                    tryIt { filterData.add(java.lang.Byte.parseByte(byte)) }
                }

                val imageData = ByteArray(filterData.size)
                for (d in imageData.indices) imageData[d] = filterData[d]

                BitmapFactory.decodeByteArray(imageData, 0, imageData.size)

            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }, {
            if (it is Bitmap) iconView.setImageBitmap(it)
            else iconView.setImageResource(R.drawable.ic_save_icon)
        })

    }

    fun setIconFromFile(iconView: ImageView, file: FileX) {

        var bitmap: Bitmap? = null
        val stringIcon = StringBuffer("")

        doBackgroundTask({
            if (file.name.endsWith(".png")) {
                tryIt { bitmap = BitmapFactory.decodeStream(file.inputStream()) }
            }
            else {
                tryIt {
                    if (file.exists() && file.canRead()) {
                        file.readLines().forEach {
                            stringIcon.append(it)
                        }
                    }
                }
            }
            return@doBackgroundTask stringIcon.toString()
        }, {
            if (bitmap != null) tryIt { iconView.setImageBitmap(bitmap) }
            else setIconFromIconString(iconView, it.toString())
        })

    }
}