package balti.migrate.extraBackupsActivity.apps

import android.app.ActivityManager
import android.app.NotificationManager
import android.app.usage.StorageStatsManager
import android.content.Context
import android.content.Context.*
import android.content.pm.IPackageStatsObserver
import android.content.pm.PackageStats
import android.os.AsyncTask
import android.os.Build
import android.os.StatFs
import android.support.v7.app.AlertDialog
import android.util.Log
import android.view.View
import balti.migrate.R
import balti.migrate.backupActivity.containers.BackupDataPacketKotlin
import balti.migrate.extraBackupsActivity.apps.containers.AppBatch
import balti.migrate.extraBackupsActivity.apps.containers.AppPacket
import balti.migrate.extraBackupsActivity.utils.OnJobCompletion
import balti.migrate.extraBackupsActivity.utils.ViewOperations
import balti.migrate.utilities.CommonToolKotlin
import balti.migrate.utilities.CommonToolKotlin.Companion.DEBUG_TAG
import balti.migrate.utilities.CommonToolKotlin.Companion.MAX_TWRP_ZIP_SIZE
import balti.migrate.utilities.CommonToolKotlin.Companion.PREF_ALTERNATE_METHOD
import balti.migrate.utilities.CommonToolKotlin.Companion.PREF_CALCULATING_SIZE_METHOD
import balti.migrate.utilities.CommonToolKotlin.Companion.PREF_FILE_MAIN
import balti.migrate.utilities.CommonToolKotlin.Companion.PREF_MAX_BACKUP_SIZE
import balti.migrate.utilities.CommonToolKotlin.Companion.PREF_TERMINAL_METHOD
import kotlinx.android.synthetic.main.please_wait.view.*
import java.io.*
import kotlin.math.ceil

class MakeAppPackets(private val jobCode: Int, private val context: Context, private val destination: String,
                     private val appList: ArrayList<BackupDataPacketKotlin> = ArrayList(0)):
        AsyncTask<Any, String, Array<Any>>() {

    private val dialogView by lazy { View.inflate(context, R.layout.please_wait, null) }
    private val waitingDialog by lazy { AlertDialog.Builder(context)
            .setView(dialogView)
            .setCancelable(false)
            .create()
    }
    private val onJobCompletion by lazy { context as OnJobCompletion }
    private val vOp by lazy { ViewOperations(context) }
    private val appBatches by lazy { ArrayList<AppBatch>(0) }
    private val parentAppBatch by lazy { ArrayList<AppPacket>(0) }
    private val notificationManager by lazy { context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager }
    private val main by lazy { context.getSharedPreferences(PREF_FILE_MAIN, MODE_PRIVATE) }
    private val commonTools by lazy { CommonToolKotlin(context) }
    private val pm by lazy { context.packageManager }
    private val maxBackupSize : Long
        get() = context.getSharedPreferences(PREF_FILE_MAIN, MODE_PRIVATE).getLong(PREF_MAX_BACKUP_SIZE, MAX_TWRP_ZIP_SIZE)

    private var availableKb = 0L
    private var totalMemory = 0L
    private var totalSize = 0L

    init {
        dialogView.waiting_cancel.setOnClickListener {
            vOp.doSomething { cancel(true) }
            vOp.doSomething { waitingDialog.dismiss() }
        }
        vOp.visibilitySet(dialogView.waiting_progress, View.GONE)
        vOp.visibilitySet(dialogView.waiting_details, View.GONE)
    }

    override fun onPreExecute() {
        super.onPreExecute()
        vOp.doSomething {
            waitingDialog.show()
            notificationManager.cancelAll()
            (context.filesDir.listFiles() + context.externalCacheDir.listFiles()).forEach {
                it.delete()
            }
        }
    }

    private fun calculateSizesByTerminalMethod(): String {

        Log.d(DEBUG_TAG, "Method terminal")

        val busyboxPath: String = Build.SUPPORTED_ABIS[0].run {
            if (this == "armeabi-v7a" || this == "arm64-v8a")
                commonTools.unpackAssetToInternal("busybox", "busybox", true)
            else if (this == "x86" || this == "x86_64")
                commonTools.unpackAssetToInternal("busybox-x86", "busybox", true)
            else ""
        }

        Log.d(DEBUG_TAG, "du: $busyboxPath")

        if (busyboxPath.trim() == "") return vOp.getStringFromRes(R.string.no_busybox_binary)

        val processMemoryFinder = Runtime.getRuntime().exec("su")
        val processReader = BufferedReader(InputStreamReader(processMemoryFinder.inputStream))
        val processWriter = BufferedWriter(OutputStreamWriter(processMemoryFinder.outputStream))

        for (i in 0 until appList.size) {

            var dataSize = 0L
            var systemSize = 0L
            val dp = appList[i]

            val apkPath: String? = if (dp.APP)
                dp.PACKAGE_INFO.applicationInfo.sourceDir
            else null

            val dataPath: String? = if (dp.DATA)
                dp.PACKAGE_INFO.applicationInfo.dataDir
            else null

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
            }

            parentAppBatch.add(AppPacket(dp, dataSize, systemSize))
            totalSize += dataSize + systemSize

            publishProgress(vOp.getStringFromRes(R.string.calculating_size),
                    (i + 1).toString() + " of " + appList.size,
                    pm.getApplicationLabel(dp.PACKAGE_INFO.applicationInfo).toString() + "\n" +
                            vOp.getStringFromRes(R.string.estimated_files_size) + " " + commonTools.getHumanReadableStorageSpace(systemSize + dataSize) + "\n")

        }

        processWriter.write("exit\n")
        processWriter.flush()

        return ""
    }

    override fun doInBackground(vararg params: Any?): Array<Any> {

        var method = PREF_ALTERNATE_METHOD
        vOp.doSomething {
            method = main.getInt(PREF_CALCULATING_SIZE_METHOD, PREF_ALTERNATE_METHOD)
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

                    val getPackageSizeInfo = pm.javaClass.getMethod(
                            "getPackageSizeInfo", String::class.java, IPackageStatsObserver::class.java)

                    for (i in 0 until appList.size){

                        val dp = appList[i]

                        getPackageSizeInfo.invoke(pm, dp.PACKAGE_INFO, object : IPackageStatsObserver.Stub(){
                            override fun onGetStatsCompleted(pStats: PackageStats?, succeeded: Boolean) {

                                var dataSize = 0L
                                var systemSize = 0L

                                val apkPath: String? = if (dp.APP)
                                    dp.PACKAGE_INFO.applicationInfo.sourceDir
                                else null

                                val dataPath: String? = if (dp.DATA)
                                    dp.PACKAGE_INFO.applicationInfo.dataDir
                                else null

                                apkPath?.let {apk ->
                                    if (apk.startsWith("/system"))
                                        systemSize += File(apk).length()
                                    else dataSize += File(apk).length()
                                }

                                dataPath?.let { _ ->
                                    pStats?.let {
                                        dataSize += it.dataSize + it.cacheSize
                                    }
                                }

                                dataSize /= 1024
                                systemSize /= 1024

                                parentAppBatch.add(AppPacket(dp, dataSize, systemSize))
                                totalSize += dataSize + systemSize

                                publishProgress(vOp.getStringFromRes(R.string.calculating_size),
                                        (i + 1).toString() + " of " + appList.size,
                                        pm.getApplicationLabel(dp.PACKAGE_INFO.applicationInfo).toString() + "\n" +
                                                vOp.getStringFromRes(R.string.estimated_files_size) + " " + commonTools.getHumanReadableStorageSpace(systemSize+dataSize) + "\n")

                            }
                        })

                    }

                    while (parentAppBatch.size < appList.size)
                        Thread.sleep(100)

                } else {

                    val storageStatsManager = context.getSystemService(STORAGE_STATS_SERVICE) as StorageStatsManager

                    for (i in 0 until appList.size){

                        val dp = appList[i]
                        var dataSize = 0L
                        var systemSize = 0L

                        val storageStats = storageStatsManager.queryStatsForUid(
                                dp.PACKAGE_INFO.applicationInfo.storageUuid,
                                dp.PACKAGE_INFO.applicationInfo.uid)

                        val apkPath: String? = if (dp.APP)
                            dp.PACKAGE_INFO.applicationInfo.sourceDir
                        else null

                        val dataPath: String? = if (dp.DATA)
                            dp.PACKAGE_INFO.applicationInfo.dataDir
                        else null

                        val externalData = File("/sdcard/Android/data/" + dp.PACKAGE_INFO.packageName)
                        val externalMedia = File("/sdcard/Android/media/" + dp.PACKAGE_INFO.packageName)

                        apkPath?.let {apk ->
                            if (apk.startsWith("/system"))
                                systemSize += File(apk).length()
                            else dataSize += File(apk).length()
                        }

                        dataPath?.let {
                            val ignoreSize =
                                    (if (externalData.canRead()) commonTools.getDirLength(externalData.absolutePath) else 0) +
                                            (if (externalMedia.canRead()) commonTools.getDirLength(externalMedia.absolutePath) else 0)

                            dataSize += storageStats.dataBytes - ignoreSize
                        }

                        dataSize /= 1024
                        systemSize /= 1024

                        parentAppBatch.add(AppPacket(dp, dataSize, systemSize))
                        totalSize += dataSize + systemSize

                        publishProgress(vOp.getStringFromRes(R.string.calculating_size),
                                (i + 1).toString() + " of " + appList.size,
                                pm.getApplicationLabel(dp.PACKAGE_INFO.applicationInfo).toString() + "\n" +
                                        vOp.getStringFromRes(R.string.estimated_files_size) + " " + commonTools.getHumanReadableStorageSpace(systemSize+dataSize) + "\n")

                    }

                }

            }

        }
        catch (e: Exception){
            e.printStackTrace()
            return arrayOf(false, vOp.getStringFromRes(R.string.error_calculating_size), e.message + "\n\n" + vOp.getStringFromRes(R.string.change_size_calculation_method))
        }

        try {

            if (parentAppBatch.size != appList.size && method == PREF_ALTERNATE_METHOD){

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

        publishProgress(vOp.getStringFromRes(R.string.making_batches), "", "")

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
                availableKb = it.blockSizeLong * it.availableBlocksLong / 1024
            }

            val activityManager = context.getSystemService(ACTIVITY_SERVICE) as ActivityManager
            ActivityManager.MemoryInfo().let {
                activityManager.getMemoryInfo(it)
                totalMemory = it.totalMem / 1024
            }

            (if (maxBackupSize >= MAX_TWRP_ZIP_SIZE) MAX_TWRP_ZIP_SIZE else maxBackupSize).let {
                totalMemory = if (totalMemory >= it) it
                else totalMemory
            }

        }
        catch (e: Exception){
            e.printStackTrace()
            return arrayOf(false, vOp.getStringFromRes(R.string.error_detecting_memory), e.message.toString())
        }

        if (totalSize > availableKb){
            return arrayOf(false, vOp.getStringFromRes(R.string.insufficient_storage),
                    "${vOp.getStringFromRes(R.string.estimated_files_size)} ${commonTools.getHumanReadableStorageSpace(totalSize)}\n" +
                            "${vOp.getStringFromRes(R.string.available_space)} ${commonTools.getHumanReadableStorageSpace(availableKb)}\n\n" +
                            "${vOp.getStringFromRes(R.string.required_storage)} ${commonTools.getHumanReadableStorageSpace(totalSize - availableKb)}\n\n" +
                            vOp.getStringFromRes(R.string.will_be_compressed))
        }

        try {

            val parts = ceil((totalSize * 1.0) / totalMemory).toInt()

            for (i in 1..parts) {

                val batchPackets = ArrayList<AppPacket>(0)
                var batchSize = 0L
                var c = 0

                while (c < parentAppBatch.size && batchSize < totalMemory) {

                    val dp = parentAppBatch[c]
                    if (batchSize + dp.systemSize + dp.dataSize <= totalMemory) {
                        batchPackets.add(dp)
                        batchSize += dp.dataSize + dp.systemSize
                        parentAppBatch.removeAt(c)
                    } else c++
                }

                if (batchSize == 0L && parentAppBatch.size != 0) {
                    // signifies that all apps were considered but none could be added to a batch due to memory constraints

                    var concatenatedNames = ""
                    for (dp in parentAppBatch) concatenatedNames += "${pm.getApplicationLabel(dp.PACKAGE_INFO.applicationInfo)}\n"

                    return arrayOf(false, vOp.getStringFromRes(R.string.cannot_split), concatenatedNames)
                } else appBatches.add(AppBatch(batchPackets))

            }

            return arrayOf(true)
        }
        catch (e: Exception){
            e.printStackTrace()
            return arrayOf(false, vOp.getStringFromRes(R.string.error_making_batches), e.message.toString())
        }
    }

    override fun onProgressUpdate(vararg values: String?) {
        super.onProgressUpdate(*values)
        vOp.visibilitySet(dialogView.waiting_progress, View.VISIBLE)
        vOp.visibilitySet(dialogView.waiting_details, View.VISIBLE)
        values[0]?.let {vOp.textSet(dialogView.waiting_head, it.trim())}
        values[1]?.let {vOp.textSet(dialogView.waiting_progress, it.trim())}
        values[2]?.let {vOp.textSet(dialogView.waiting_details, it.trim())}
    }

    override fun onPostExecute(result: Array<Any>) {
        super.onPostExecute(result)

        vOp.visibilitySet(dialogView.waiting_cancel, View.GONE)

        try { waitingDialog.dismiss() } catch (e: Exception){}

        vOp.doSomething {

            if (!(result[0] as Boolean)) {

                val errorDialog = AlertDialog.Builder(context)
                        .setTitle(result[1] as String)
                        .setMessage(result[2] as String)
                        .setPositiveButton(R.string.close, null)
                        .create()

                if (result[1] as String == vOp.getStringFromRes(R.string.insufficient_storage))
                    errorDialog.setIcon(R.drawable.ic_combine)
                else errorDialog.setIcon(R.drawable.ic_cancelled)

                errorDialog.show()
                onJobCompletion.onComplete(jobCode, false, result[1] as String)
            } else {
                onJobCompletion.onComplete(jobCode, true, appBatches)
            }
        }
    }

    override fun onCancelled() {
        super.onCancelled()
        vOp.doSomething { waitingDialog.dismiss() }
    }
}