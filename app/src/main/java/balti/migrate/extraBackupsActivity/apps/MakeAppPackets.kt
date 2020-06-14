package balti.migrate.extraBackupsActivity.apps

import android.app.NotificationManager
import android.app.usage.StorageStatsManager
import android.content.Context
import android.content.Context.NOTIFICATION_SERVICE
import android.content.Context.STORAGE_STATS_SERVICE
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import balti.migrate.AppInstance
import balti.migrate.AppInstance.Companion.MAX_WORKING_SIZE
import balti.migrate.AppInstance.Companion.RESERVED_SPACE
import balti.migrate.R
import balti.migrate.extraBackupsActivity.apps.containers.AppPacket
import balti.migrate.extraBackupsActivity.utils.OnJobCompletion
import balti.migrate.extraBackupsActivity.utils.ViewOperations
import balti.migrate.utilities.CommonToolsKotlin
import balti.migrate.utilities.CommonToolsKotlin.Companion.DEBUG_TAG
import balti.migrate.utilities.CommonToolsKotlin.Companion.PREF_ALTERNATE_METHOD
import balti.migrate.utilities.CommonToolsKotlin.Companion.PREF_CALCULATING_SIZE_METHOD
import balti.migrate.utilities.CommonToolsKotlin.Companion.PREF_IGNORE_APP_CACHE
import balti.migrate.utilities.CommonToolsKotlin.Companion.PREF_TERMINAL_METHOD
import balti.module.baltitoolbox.functions.FileHandlers.getDirLength
import balti.module.baltitoolbox.functions.FileHandlers.unpackAssetToInternal
import balti.module.baltitoolbox.functions.Misc.getHumanReadableStorageSpace
import balti.module.baltitoolbox.functions.Misc.tryIt
import balti.module.baltitoolbox.functions.SharedPrefs.getPrefBoolean
import balti.module.baltitoolbox.functions.SharedPrefs.getPrefInt
import balti.module.baltitoolbox.jobHandlers.AsyncCoroutineTask
import kotlinx.android.synthetic.main.please_wait.view.*
import java.io.*

class MakeAppPackets(private val jobCode: Int, private val context: Context, private val destination: String, private val dialogView: View):
        AsyncCoroutineTask() {

    private val onJobCompletion by lazy { context as OnJobCompletion }
    private val vOp by lazy { ViewOperations(context) }
    private val notificationManager by lazy { context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager }
    private val commonTools by lazy { CommonToolsKotlin(context) }
    private val pm by lazy { context.packageManager }
    private val appPackets by lazy { ArrayList<AppPacket>(0) }
    private val appList by lazy { AppInstance.appBackupDataPackets }

    private var appsScanned = 0

    private val ignoreCache by lazy { getPrefBoolean(PREF_IGNORE_APP_CACHE, false) }

    private var availableBytes = 0L
    private var totalSize = 0L
    private var lastSize = 0L
    private var lastAppInfo = ""

    private var cancelThis = false

    override suspend fun onPreExecute() {
        super.onPreExecute()
        vOp.doSomething {
            notificationManager.cancelAll()
            (context.filesDir.listFiles() + context.externalCacheDir.let { if(it != null) it.listFiles() else arrayOf() }).forEach {
                it.delete()
            }
        }

        dialogView.waiting_cancel.apply {

            visibility = View.VISIBLE
            setText(android.R.string.cancel)
            val cancellingText = vOp.getStringFromRes(R.string.cancelling)

            setOnClickListener {
                cancelThis = true
                text = cancellingText
                Toast.makeText(context, R.string.long_press_to_force_stop, Toast.LENGTH_SHORT).show()
            }
            setOnLongClickListener {
                if (text == cancellingText) commonTools.forceCloseThis()
                true
            }
        }
        vOp.visibilitySet(dialogView.waiting_progress, View.GONE)
        vOp.visibilitySet(dialogView.waiting_details, View.GONE)

        totalSize = 0
        lastSize = 0
        lastAppInfo = ""
        appsScanned = 0
        appPackets.clear()
    }

    private fun calculateSizesByTerminalMethod(): String {

        Log.d(DEBUG_TAG, "Method terminal")

        val busyboxPath: String = Build.SUPPORTED_ABIS[0].run {
            (if (this == "x86" || this == "x86_64")
                unpackAssetToInternal("busybox-86", "busybox")
            else unpackAssetToInternal("busybox")).apply {
                tryIt { File(this).setExecutable(true) }
            }
        }

        Log.d(DEBUG_TAG, "du: $busyboxPath")

        if (busyboxPath.trim() == "") return vOp.getStringFromRes(R.string.no_busybox_binary)

        val processMemoryFinder = Runtime.getRuntime().exec("su")
        val processReader = BufferedReader(InputStreamReader(processMemoryFinder.inputStream))
        val processWriter = BufferedWriter(OutputStreamWriter(processMemoryFinder.outputStream))

        for (i in 0 until appList.size) {

            if (cancelThis) break

            lastAppInfo = if (i > 0){
                try {
                    "${vOp.getStringFromRes(R.string.last_app)} " +
                            "${commonTools.applyNamingCorrectionForDisplay(pm.getApplicationLabel(appList[i - 1].PACKAGE_INFO.applicationInfo).toString())} -> " +
                            getHumanReadableStorageSpace(lastSize)
                }
                catch (e: Exception) {"Error: ${e.message}"}
            }
            else ""

            var dataSize = 0L
            var systemSize = 0L
            val dp = appList[i]
            val appName = commonTools.applyNamingCorrectionForDisplay(pm.getApplicationLabel(dp.PACKAGE_INFO.applicationInfo).toString())

            publishProgress(vOp.getStringFromRes(R.string.calculating_size),
                    (i + 1).toString() + " of " + appList.size,
                    "${vOp.getStringFromRes(R.string.current_app)} $appName\n" +
                            "$lastAppInfo\n" +
                            "${vOp.getStringFromRes(R.string.calculated_total)} ${getHumanReadableStorageSpace(totalSize)}")

            val apkPath: String? = if (dp.APP)
                dp.PACKAGE_INFO.applicationInfo.sourceDir
            else null

            val dataPath: String? = if (dp.DATA)
                dp.PACKAGE_INFO.applicationInfo.dataDir
            else null

            val dataCachePath = "$dataPath/cache"

            apkPath?.let { apk ->
                processWriter.write("$busyboxPath du -s $apk\n")
                processWriter.flush()
                processReader.readLine().let { read ->
                    read.substring(0, read.indexOf("/")).trim().toLongOrNull()?.let { size ->
                        if (apk.startsWith("/system"))
                            systemSize += size
                        else dataSize += size
                    }
                }
            }

            dataPath?.let { data ->
                processWriter.write("$busyboxPath du -s $data\n")
                processWriter.flush()
                processReader.readLine().let { read ->
                    read.substring(0, read.indexOf("/")).trim().toLongOrNull()?.let { size ->
                        dataSize += size
                    }
                }

                if (ignoreCache){
                    processWriter.write("$busyboxPath du -s $dataCachePath\n")
                    processWriter.flush()
                    processReader.readLine().let { read ->
                        read.substring(0, read.indexOf("/")).trim().toLongOrNull()?.let { size ->
                            dataSize -= size
                        }
                    }
                }
            }

            // du shows files in KB, but rest of the calculations in the whole app is in bytes.
            // so convert KB to bytes
            dataSize *= 1024
            systemSize *= 1024

            appsScanned++
            appPackets.add(AppPacket(dp, appName, dataSize, systemSize))
            lastSize = dataSize + systemSize
            totalSize += lastSize

        }

        processWriter.write("exit\n")
        processWriter.flush()

        return ""
    }

    override suspend fun doInBackground(arg: Any?): Any? {

        var method = PREF_ALTERNATE_METHOD
        vOp.doSomething {
            method = getPrefInt(PREF_CALCULATING_SIZE_METHOD, PREF_ALTERNATE_METHOD)
        }

        publishProgress(vOp.getStringFromRes(R.string.calculating_size), "", "")

        try {

            if (method == PREF_TERMINAL_METHOD){

                calculateSizesByTerminalMethod().trim().let {
                    if (it != "") throw Exception(it)
                }

            }
            else if (method == PREF_ALTERNATE_METHOD){

                Log.d(DEBUG_TAG, "Method alternate")

                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O){

                    tryIt {
                        doOnMainThreadParallel {
                            Toast.makeText(context, R.string.new_method_not_available_below_oreo, Toast.LENGTH_SHORT).show()
                        }
                    }

                    calculateSizesByTerminalMethod().trim().let {
                        if (it != "") throw Exception(it)
                    }

                } else {

                    val storageStatsManager = context.getSystemService(STORAGE_STATS_SERVICE) as StorageStatsManager

                    for (i in 0 until appList.size){

                        if (cancelThis) break

                        lastAppInfo = if (i > 0){
                            try {
                                "${vOp.getStringFromRes(R.string.last_app)} " +
                                        "${commonTools.applyNamingCorrectionForDisplay(pm.getApplicationLabel(appList[i - 1].PACKAGE_INFO.applicationInfo).toString())} -> " +
                                        getHumanReadableStorageSpace(lastSize)
                            }
                            catch (e: Exception) {"Error: ${e.message}"}
                        }
                        else ""

                        var dataSize = 0L
                        var systemSize = 0L
                        val dp = appList[i]
                        val appName = commonTools.applyNamingCorrectionForDisplay(pm.getApplicationLabel(dp.PACKAGE_INFO.applicationInfo).toString())

                        publishProgress(vOp.getStringFromRes(R.string.calculating_size),
                                (i + 1).toString() + " of " + appList.size,
                                "${vOp.getStringFromRes(R.string.current_app)} $appName\n" +
                                        "$lastAppInfo\n" +
                                        "${vOp.getStringFromRes(R.string.calculated_total)} ${getHumanReadableStorageSpace(totalSize)}")

                        val storageStats = storageStatsManager.queryStatsForUid(
                                dp.PACKAGE_INFO.applicationInfo.storageUuid,
                                dp.PACKAGE_INFO.applicationInfo.uid)

                        val apkPath: String? = if (dp.APP)
                            dp.PACKAGE_INFO.applicationInfo.sourceDir
                        else null

                        val dataPath: String? = if (dp.DATA)
                            dp.PACKAGE_INFO.applicationInfo.dataDir
                        else null

                        val sdcard = Environment.getExternalStorageDirectory().path
                        val externalData = File("$sdcard/Android/data/" + dp.PACKAGE_INFO.packageName)
                        val externalMedia = File("$sdcard/Android/media/" + dp.PACKAGE_INFO.packageName)

                        apkPath?.let {apk ->
                            if (apk.startsWith("/system"))
                                systemSize += File(apk).length()
                            else dataSize += File(apk).length()
                        }

                        dataPath?.let {
                            val ignoreSize =
                                    (if (externalData.canRead()) getDirLength(externalData.absolutePath) else 0) +
                                            (if (externalMedia.canRead()) getDirLength(externalMedia.absolutePath) else 0)

                            dataSize += storageStats.dataBytes - ignoreSize
                            if (ignoreCache) dataSize -= storageStats.cacheBytes

                        }

                        appsScanned++
                        appPackets.add(AppPacket(dp, appName, dataSize, systemSize))
                        lastSize = dataSize + systemSize
                        totalSize += lastSize

                    }

                }

            }

        }
        catch (e: Exception){
            e.printStackTrace()
            return arrayOf(false, vOp.getStringFromRes(R.string.error_calculating_size), e.message + "\n\n" + vOp.getStringFromRes(R.string.change_size_calculation_method))
        }

        try {

            if (appsScanned != appList.size && method == PREF_ALTERNATE_METHOD){

                publishProgress(vOp.getStringFromRes(R.string.re_calculating_size), "", "")

                calculateSizesByTerminalMethod().trim().let {
                    if (it != "") throw Exception(it)
                }
            }
        }
        catch (e: Exception){
            e.printStackTrace()
            return arrayOf(false, vOp.getStringFromRes(R.string.error_calculating_size), e.message + "\n\n" + vOp.getStringFromRes(R.string.change_size_calculation_method))
        }

        // check if any app is there which is too large before comparing with device storage
        val bigAppsNameConcat = StringBuilder("")
        appPackets.forEach {
            if ((it.dataSizeBytes + it.systemSizeBytes) > (MAX_WORKING_SIZE - RESERVED_SPACE)) bigAppsNameConcat.append("${it.appName}\n")
        }
        if (bigAppsNameConcat.toString().trim() != ""){
            return arrayOf(false, vOp.getStringFromRes(R.string.cannot_split), bigAppsNameConcat.toString())
        }

        try {
            File(destination).let {
                it.mkdirs()
                if (!it.canWrite())
                    return arrayOf(false, vOp.getStringFromRes(R.string.could_not_create_destination), destination + "\n\n" + vOp.getStringFromRes(R.string.make_sure_destination_exists))
            }
        }
        catch (e: Exception){
            e.printStackTrace()
            return arrayOf(false, vOp.getStringFromRes(R.string.error_checking_destination), e.message.toString())
        }

        try {

            StatFs(destination).let {
                availableBytes = it.blockSizeLong * it.availableBlocksLong
            }

        }
        catch (e: Exception){
            e.printStackTrace()
            return arrayOf(false, vOp.getStringFromRes(R.string.error_detecting_memory), e.message.toString())
        }

        Log.d(DEBUG_TAG, "Total size: $totalSize, availableBytes: $availableBytes")

        return if (totalSize > availableBytes){
            arrayOf(false, vOp.getStringFromRes(R.string.insufficient_storage),
                    "${vOp.getStringFromRes(R.string.estimated_files_size)} ${getHumanReadableStorageSpace(totalSize)}\n" +
                            "${vOp.getStringFromRes(R.string.available_space)} ${getHumanReadableStorageSpace(availableBytes)}\n\n" +
                            "${vOp.getStringFromRes(R.string.required_storage)} ${getHumanReadableStorageSpace(totalSize - availableBytes)}\n\n" +
                            vOp.getStringFromRes(R.string.will_be_compressed))
        }
        else {
            publishProgress(vOp.getStringFromRes(R.string.total_size_ok),
                    "${vOp.getStringFromRes(R.string.total_size)} ${getHumanReadableStorageSpace(totalSize)}\n" +
                            "${vOp.getStringFromRes(R.string.available_space)} ${getHumanReadableStorageSpace(availableBytes)}", "")
            sleepTask(1000)
            arrayOf(true)
        }
    }

    override suspend fun onProgressUpdate(vararg values: Any) {
        super.onProgressUpdate(*values)
        if (!cancelThis) {
            vOp.visibilitySet(dialogView.waiting_progress, View.VISIBLE)
            vOp.visibilitySet(dialogView.waiting_details, View.VISIBLE)
            values.run {
                0.let {if (size > it) vOp.textSet(dialogView.waiting_head, this[it].toString().trim()) }
                1.let {if (size > it) vOp.textSet(dialogView.waiting_progress, this[it].toString().trim()) }
                2.let {if (size > it) vOp.textSet(dialogView.waiting_details, this[it].toString().trim()) }
            }
        }
    }

    override suspend fun onPostExecute(result: Any?) {
        super.onPostExecute(result)

        if (result !is Array<*>) return

        if (cancelThis) onJobCompletion.onComplete(jobCode, false, "")
        else {
            vOp.visibilitySet(dialogView.waiting_cancel, View.GONE)

            try {

                if (!(result[0] as Boolean)) {

                    val errorDialog = AlertDialog.Builder(context)
                            .setTitle(result[1].toString())
                            .setMessage(result[2].toString())
                            .setPositiveButton(R.string.close, null)
                            .create()

                    if (result[1] as String == vOp.getStringFromRes(R.string.insufficient_storage))
                        errorDialog.setIcon(R.drawable.ic_zipping_icon)
                    else errorDialog.setIcon(R.drawable.ic_cancelled_icon)

                    errorDialog.show()
                    onJobCompletion.onComplete(jobCode, false, result[1] as String)
                } else {
                    onJobCompletion.onComplete(jobCode, true, appPackets)
                }
            }
            catch (e: Exception){
                e.printStackTrace()
                Toast.makeText(context, "App size postExecute: ${e.message.toString()}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}