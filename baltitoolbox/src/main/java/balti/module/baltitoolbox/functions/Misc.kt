package balti.module.baltitoolbox.functions

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import balti.module.baltitoolbox.R
import balti.module.baltitoolbox.ToolboxHQ
import balti.module.baltitoolbox.functions.GetResources.getStringFromRes
import balti.module.baltitoolbox.jobHandlers.AsyncCoroutineTask
import java.io.BufferedReader
import java.util.*

object Misc {

    private val context = ToolboxHQ.context

    fun tryIt(f: () -> Any?, showError: Boolean = false): Any? {
        return try { f() } catch (e: Exception) {
            if (showError) showErrorDialog(e.message.toString())
            e
        }
    }

    fun tryIt(f: () -> Any?): Any? = tryIt(f, false)

    fun tryIt(f: () -> Unit, showError: Boolean = false) {
        try { f() } catch (e: Exception) {
            if (showError) showErrorDialog(e.message.toString())
        }
    }

    fun tryIt(f: () -> Unit) = tryIt(f, false)

    fun isPackageInstalled(packageName: String): Boolean{
        return try {
            context.packageManager.getPackageInfo(packageName, PackageManager.GET_META_DATA)
            true
        }
        catch (_: Exception){ false }
    }

    fun getAppName(packageName: String): String {
        return if (isPackageInstalled(packageName)){
            context.packageManager.getApplicationLabel(
                    context.packageManager.getPackageInfo(
                            packageName,
                            PackageManager.GET_META_DATA
                    ).applicationInfo
            ).toString()
        }
        else ""
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

    fun showErrorDialog(message: String, title: String = "", activityContext: Context? = null,
                        iconResource: Int = 0, negativeButtonText: String = getStringFromRes(R.string.close), onCloseClick: (() -> Unit)? = null){

        val workingContext = activityContext?: this.context

        try {
            AlertDialog.Builder(workingContext)
                    .setIcon(if (iconResource == 0) android.R.drawable.stat_sys_warning else iconResource)
                    .setMessage(message).apply {

                        setNegativeButton(negativeButtonText) { _, _ -> onCloseClick?.invoke() }

                        setCancelable(onCloseClick == null)

                        if (title == "")
                            setTitle(R.string.error_occurred)
                        else setTitle(title)

                    }
                    .show()
        } catch (e: Exception){
            e.printStackTrace()
            tryIt { Toast.makeText(context, message, Toast.LENGTH_SHORT).show() }
        }
    }

    fun iterateBufferedReader(reader: BufferedReader, loopFunction: (line: String) -> Boolean,
                              onExitFunction: (() -> Unit)? = null){
        var doBreak = false
        while (true){
            val line : String? = reader.readLine()
            if (line == null) break
            else {
                doBreak = loopFunction(line.trim())
                if (doBreak) break
            }
        }
        if (doBreak) onExitFunction?.invoke()
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

    fun getPercentage(count: Int, total: Int): Int {
        return if (total != 0) (count*100)/total
        else 0
    }

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