package balti.updater

import android.content.Context
import android.content.Context.CONNECTIVITY_SERVICE
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import java.io.File

class Tools(val context: Context? = null) {

    internal fun tryIt(f: () -> Unit){
        try { f() } catch (e: Exception){}
    }


    internal fun getHumanReadableStorageSpace(space: Long): String {
        var res = "B"

        var s = space.toDouble()

        if (s > 1024) {
            s /= 1024.0
            res = "KB"
        }
        if (s > 1024) {
            s /= 1024.0
            res = "MB"
        }
        if (s > 1024) {
            s /= 1024.0
            res = "GB"
        }

        return String.format("%.2f", s) + " " + res
    }

    internal fun getUri(file: File) =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                FileProvider.getUriForFile(Updater.context, "migrate.provider", file)
            else Uri.fromFile(file)

    internal fun isInternetAvailable(): Boolean {
        (Updater.context.getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager).let { conMan ->
            val netInfo = conMan.activeNetworkInfo
            return netInfo != null
        }
    }

    internal fun validateError(e: String): String{
        return if (e != "") {
            if (isInternetAvailable()) e
            else Updater.context.getString(R.string.no_internet)
        }
        else e
    }
}