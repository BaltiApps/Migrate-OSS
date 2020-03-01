package balti.updater

import android.content.Context

class Tools(val context: Context? = null) {

    fun tryIt(f: () -> Unit){
        try { f() } catch (e: Exception){}
    }


    fun getHumanReadableStorageSpace(space: Long): String {
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
}