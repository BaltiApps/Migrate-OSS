package balti.module.baltitoolbox.functions

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.content.pm.PackageManager
import android.graphics.Color
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.util.*
import kotlin.math.roundToInt

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

    fun getHumanReadableTime(timeInSec: Long): String {

        var timeStamp = ""
        var s = timeInSec

        fun divide(currentUnit: String, nextUnitDivider: Int = 60){
            if (nextUnitDivider > 0) {
                if (s >= nextUnitDivider) {
                    val q = s / nextUnitDivider
                    val r = s - (q * nextUnitDivider)
                    if (r > 0) timeStamp = "$r $currentUnit $timeStamp".trim()
                    s = q
                } else if (s > 0) {
                    timeStamp = "$s $currentUnit $timeStamp".trim()
                    s /= nextUnitDivider
                }
            }
            else if (s > 0) timeStamp = "$s $currentUnit $timeStamp".trim()
        }

        divide("sec")
        divide("min")
        divide("hr", 24)
        divide("day", -1)

        return timeStamp
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

    fun delayTask(delayInMillis: Long, job: () -> Any?){
        class Class : AsyncCoroutineTask(){
            override suspend fun doInBackground(arg: Any?): Any? {
                sleepTask(delayInMillis)
                return 0
            }

            override suspend fun onPostExecute(result: Any?) {
                super.onPostExecute(result)
                job()
            }
        }
        Class().execute()
    }

    fun runSuspendFunction(f: () -> Unit){
        CoroutineScope(AsyncCoroutineTask.DISP_DEF).launch {
            f()
        }
    }

    fun timeInMillis() = Calendar.getInstance().timeInMillis

    fun getPercentage(count: Int, total: Int, floor: Int = 0): Int {
        if (total == 0 && floor == 0) return 0
        val amount = count - floor
        val fullAmount = total - floor
        return if (fullAmount != 0) (amount*100)/fullAmount else 0
    }

    fun getCountFromPercentage(percentage: Int, total: Int, floor: Int = 0): Int{
        val fullAmount = total - floor
        return (floor + ((percentage*fullAmount)/100.0)).roundToInt()
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

    fun activityStart(packageContext: Context, cls: Class<*>){
        packageContext.startActivity(Intent(packageContext, cls))
    }

    fun serviceStart(packageContext: Context, cls: Class<*>, isForeground: Boolean = true){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && isForeground)
            packageContext.startForegroundService(Intent(packageContext, cls))
        else packageContext.startService(Intent(packageContext, cls))
    }

    fun transparentColor(color: Int, opacityPercentage: Int, fromCurrentOpacity: Boolean = false): Int {
        val initialAlpha = if (fromCurrentOpacity) Color.alpha(color) else 0
        return Color.argb(initialAlpha + (((255 - initialAlpha) * opacityPercentage) / 100),
            Color.red(color), Color.green(color), Color.blue(color))
    }

    fun darkenColor(color: Int, amount: Int): Int {
        fun dec(color: Int): Int = (color - amount).let { if (it > 0) it else 0 }
        return Color.argb(Color.alpha(color), dec(Color.red(color)), dec(Color.green(color)), dec(Color.blue(color)))
    }


}