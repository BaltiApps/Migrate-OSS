package balti.module.baltitoolbox.functions

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import balti.module.baltitoolbox.ToolboxHQ
import balti.module.baltitoolbox.jobHandlers.AsyncCoroutineTask
import java.io.BufferedReader
import java.util.*

object Misc {

    private val context = ToolboxHQ.context

    fun tryIt(f: () -> Any?): Any? =
            try { f() } catch (e: Exception) { e }

    fun tryIt(f: () -> Unit): Unit =
            try { f() } catch (_: Exception) { }

    fun isPackageInstalled(packageName: String): Boolean{
        return try {
            context.packageManager.getPackageInfo(packageName, PackageManager.GET_META_DATA)
            true
        }
        catch (_: Exception){
            false
        }
    }

    fun getHumanReadableStorageSpace(spaceInBytes: Long, isSpaceInKB: Boolean = false): String {

        var unit = if (isSpaceInKB) "KB" else "B"

        var s = spaceInBytes.toDouble()

        fun divide(annot: String){
            if (s > 1024) {
                s /= 1024.0
                unit = annot
            }
        }

        if (!isSpaceInKB) divide("KB")
        divide("MB")
        divide("GB")
        divide("TB")

        return String.format("%.2f", s) + " " + unit
    }

    fun openWebLink(url: String) {
        if (url != "") {
            context.startActivity(Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(url)
                addFlags(FLAG_ACTIVITY_NEW_TASK)
            })
        }
    }

    fun playStoreLink(packageName: String){
        openWebLink("market://details?id=$packageName")
    }

    fun makeNotificationChannel(channelId: String, channelDesc: CharSequence, importance: Int){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(channelId, channelDesc, importance)
            channel.setSound(null, null)
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun iterateBufferedReader(reader: BufferedReader, loopFunction: (line: String) -> Boolean,
                              onCancelledFunction: (() -> Unit)? = null){
        var doBreak = false
        while (true){
            val line : String? = reader.readLine()
            if (line == null) break
            else {
                doBreak = loopFunction(line.trim())
                if (doBreak) break
            }
        }
        if (doBreak) onCancelledFunction?.invoke()
    }

    fun doBackgroundTask(job: () -> Any?, postJob: (result: Any?) -> Any?){
        class Class : AsyncCoroutineTask(){
            override suspend fun doInBackground(arg: Any?): Any? {
                return job()
            }

            override suspend fun onPostExecute(result: Any?) {
                super.onPostExecute(result)
                postJob(result)
            }
        }
        Class().execute()
    }

    fun timeInMillis() = Calendar.getInstance().timeInMillis

    fun <T> LiveData<T>.observeOnce(owner: LifecycleOwner, observer: Observer<T>, validationFunction: ((value: T) -> Boolean)? = null) {
        observe(owner, object : Observer<T> {
            override fun onChanged(t: T) {
                if (validationFunction == null || validationFunction(t)) {
                    observer.onChanged(t)
                    removeObserver(this)
                }
            }
        })
    }
}