package balti.migrate.extraBackupsActivity.apps

import android.app.usage.StorageStatsManager
import android.content.Context.STORAGE_STATS_SERVICE
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import balti.filex.FileX
import balti.filex.FileXInit
import balti.migrate.AppInstance.Companion.MAX_WORKING_SIZE
import balti.migrate.AppInstance.Companion.RESERVED_SPACE
import balti.migrate.AppInstance.Companion.appContext
import balti.migrate.AppInstance.Companion.selectedBackupDataPackets
import balti.migrate.R
import balti.migrate.extraBackupsActivity.apps.containers.AppPacket
import balti.migrate.utilities.CommonToolsKotlin
import balti.migrate.utilities.CommonToolsKotlin.Companion.DEBUG_TAG
import balti.migrate.utilities.CommonToolsKotlin.Companion.PREF_ALTERNATE_METHOD
import balti.migrate.utilities.CommonToolsKotlin.Companion.PREF_CALCULATING_SIZE_METHOD
import balti.migrate.utilities.CommonToolsKotlin.Companion.PREF_IGNORE_APP_CACHE
import balti.migrate.utilities.CommonToolsKotlin.Companion.PREF_TERMINAL_METHOD
import balti.module.baltitoolbox.functions.FileHandlers.getDirLength
import balti.module.baltitoolbox.functions.FileHandlers.unpackAssetToInternal
import balti.module.baltitoolbox.functions.GetResources.getStringFromRes
import balti.module.baltitoolbox.functions.Misc
import balti.module.baltitoolbox.functions.Misc.getHumanReadableStorageSpace
import balti.module.baltitoolbox.functions.Misc.tryIt
import balti.module.baltitoolbox.functions.SharedPrefs.getPrefBoolean
import balti.module.baltitoolbox.functions.SharedPrefs.getPrefInt
import balti.module.baltitoolbox.jobHandlers.AsyncCoroutineTask
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter

internal class MakeAppPackets(private val destination: String,
                     private val flasherOnly: Boolean,
                     private val activity: AppSizeCalculationActivity):
        AsyncCoroutineTask() {

    private val commonTools by lazy { CommonToolsKotlin() }
    private val appPackets by lazy { ArrayList<AppPacket>(0) }
    private val appList by lazy { selectedBackupDataPackets }

    private var appsScanned = 0

    private val ignoreCache by lazy { getPrefBoolean(PREF_IGNORE_APP_CACHE, false) }

    private var availableBytes = 0L
    private var totalSize = 0L
    private var lastSize = 0L
    private var lastAppInfo = ""

    var cancelThis = false

    override suspend fun onPreExecute() {
        super.onPreExecute()

        totalSize = 0
        lastSize = 0
        lastAppInfo = ""
        appsScanned = 0
        appPackets.clear()
    }

    private fun calculateSizesByTerminalMethod(): String {

        Log.d(DEBUG_TAG, "Method terminal")

        try {

            val busyboxPath: String = Build.SUPPORTED_ABIS[0].run {
                (if (this == "x86" || this == "x86_64")
                    unpackAssetToInternal("busybox-x86", "busybox")
                else unpackAssetToInternal("busybox")).apply {
                    tryIt { FileX.new(this, true).file.setExecutable(true) }
                }
            }

            Log.d(DEBUG_TAG, "du: $busyboxPath")

            if (busyboxPath.trim() == "") return getStringFromRes(R.string.no_busybox_binary)

            val processMemoryFinder = Runtime.getRuntime().exec(CommonToolsKotlin.SU_INIT)
            val processReader = BufferedReader(InputStreamReader(processMemoryFinder.inputStream))
            val processWriter = BufferedWriter(OutputStreamWriter(processMemoryFinder.outputStream))

            processWriter.write("chmod +x $busyboxPath\n")
            processWriter.flush()

            for (i in 0 until appList.size) {

                if (cancelThis) break

                lastAppInfo = if (i > 0) {
                    try {
                        "${getStringFromRes(R.string.last_app)} " +
                                "${
                                    commonTools.applyNamingCorrectionForDisplay(
                                        Misc.getAppName(
                                            appList[i - 1].PACKAGE_INFO.packageName
                                        )
                                    )
                                } -> " +
                                getHumanReadableStorageSpace(lastSize)
                    } catch (e: Exception) {
                        "Error: ${e.message}"
                    }
                } else ""

                var dataSize = 0L
                var systemSize = 0L
                val dp = appList[i]
                val appName =
                    commonTools.applyNamingCorrectionForDisplay(Misc.getAppName(dp.PACKAGE_INFO.packageName))

                publishProgress(
                    getStringFromRes(R.string.calculating_size),
                    (i + 1).toString() + " of " + appList.size,
                    "${getStringFromRes(R.string.current_app)} $appName\n" +
                            "$lastAppInfo\n" +
                            "${getStringFromRes(R.string.calculated_total)} ${
                                getHumanReadableStorageSpace(
                                    totalSize
                                )
                            }"
                )

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

                    if (ignoreCache) {
                        processWriter.write("$busyboxPath du -s $dataCachePath\n")
                        processWriter.flush()
                        processReader.readLine().let { read ->
                            read.substring(0, read.indexOf("/")).trim().toLongOrNull()
                                ?.let { size ->
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
        catch (e: Exception){
            e.printStackTrace()
            return "Terminal method error: ${e.message}"
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun calculateSizesByUsageAccess(): String {

        try {

            val storageStatsManager =
                appContext.getSystemService(STORAGE_STATS_SERVICE) as StorageStatsManager

            for (i in 0 until appList.size) {

                if (cancelThis) break

                lastAppInfo = if (i > 0) {
                    try {
                        "${getStringFromRes(R.string.last_app)} " +
                        "${commonTools.applyNamingCorrectionForDisplay(Misc.getAppName(appList[i - 1].PACKAGE_INFO.packageName))} -> " +
                        getHumanReadableStorageSpace(lastSize)
                    } catch (e: Exception) {
                        "Error: ${e.message}"
                    }
                } else ""

                var dataSize = 0L
                var systemSize = 0L
                val dp = appList[i]
                val appName =
                    commonTools.applyNamingCorrectionForDisplay(Misc.getAppName(dp.PACKAGE_INFO.packageName))

                publishProgress(
                    getStringFromRes(R.string.calculating_size),
                    (i + 1).toString() + " of " + appList.size,
                    "${getStringFromRes(R.string.current_app)} $appName\n" +
                            "$lastAppInfo\n" +
                            "${getStringFromRes(R.string.calculated_total)} ${
                                getHumanReadableStorageSpace(
                                    totalSize
                                )
                            }"
                )

                val storageStats = storageStatsManager.queryStatsForUid(
                    dp.PACKAGE_INFO.applicationInfo.storageUuid,
                    dp.PACKAGE_INFO.applicationInfo.uid
                )

                val apkPath: String? = if (dp.APP)
                    dp.PACKAGE_INFO.applicationInfo.sourceDir
                else null

                val dataPath: String? = if (dp.DATA)
                    dp.PACKAGE_INFO.applicationInfo.dataDir
                else null

                val sdcard = Environment.getExternalStorageDirectory().path
                val externalData =
                    FileX.new("$sdcard/Android/data/" + dp.PACKAGE_INFO.packageName, true)
                val externalMedia =
                    FileX.new("$sdcard/Android/media/" + dp.PACKAGE_INFO.packageName, true)

                val apkSize =
                    apkPath?.let {
                        FileX.new(it, true).let {
                            if (it.canRead()) it.length()
                            else null
                        } ?: storageStats.appBytes
                    } ?: 0

                apkPath?.let { apk ->
                    if (apk.startsWith("/system"))
                        systemSize += apkSize
                    else dataSize += apkSize
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

            return ""
        }
        catch (e: Exception){
            e.printStackTrace()
            return "Usage access method error: ${e.message}"
        }
    }

    override suspend fun doInBackground(arg: Any?): Any? {

        var method = PREF_ALTERNATE_METHOD
        method = getPrefInt(PREF_CALCULATING_SIZE_METHOD, PREF_ALTERNATE_METHOD)

        publishProgress(getStringFromRes(R.string.calculating_size), "", "")

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
                            Toast.makeText(appContext, R.string.new_method_not_available_below_oreo, Toast.LENGTH_SHORT).show()
                        }
                    }
                    calculateSizesByTerminalMethod().trim().let {
                        if (it != "") throw Exception(it)
                    }
                } else {
                    calculateSizesByUsageAccess()
                }

            }

        }
        catch (e: Exception){
            e.printStackTrace()
            return arrayOf(false, getStringFromRes(R.string.error_calculating_size), e.message + "\n\n" + getStringFromRes(R.string.change_size_calculation_method))
        }

        try {

            if (appsScanned != appList.size && method == PREF_ALTERNATE_METHOD){

                publishProgress(getStringFromRes(R.string.re_calculating_size), "", "")

                calculateSizesByTerminalMethod().trim().let {
                    if (it != "") throw Exception(it)
                }
            }
        }
        catch (e: Exception){
            e.printStackTrace()
            return arrayOf(false, getStringFromRes(R.string.error_calculating_size), e.message + "\n\n" + getStringFromRes(R.string.change_size_calculation_method))
        }

        // check if any app is there which is too large before comparing with device storage
        if (!flasherOnly) {
            val bigAppsNameConcat = StringBuilder("")
            appPackets.forEach {
                if ((it.dataSizeBytes + it.systemSizeBytes) > (MAX_WORKING_SIZE - RESERVED_SPACE)) bigAppsNameConcat.append("${it.appName}\n")
            }
            if (bigAppsNameConcat.toString().trim() != "") {
                return arrayOf(false, getStringFromRes(R.string.cannot_split),
                        "\n" + getStringFromRes(R.string.cannot_split_hint) + "\n\n" +
                                getStringFromRes(R.string.max_zip_size) + " : " + "${getHumanReadableStorageSpace(MAX_WORKING_SIZE)}" + "\n\n" +
                                getStringFromRes(R.string.too_big_apps) + "\n\n" + bigAppsNameConcat.toString()
                )
            }
        }

        try {
            if (FileXInit.isTraditional) {
                FileX.new(destination).let {
                    it.mkdirs()
                    if (!it.canWrite())
                        return arrayOf(false, getStringFromRes(R.string.could_not_create_destination), destination + "\n\n" + getStringFromRes(R.string.make_sure_destination_exists))
                }
            }
        }
        catch (e: Exception){
            e.printStackTrace()
            return arrayOf(false, getStringFromRes(R.string.error_checking_destination), e.message.toString())
        }

        try {

            tryIt {
                StatFs(destination).let {
                    availableBytes = it.blockSizeLong * it.availableBlocksLong
                }
            }

            if (availableBytes == 0L) {
                availableBytes = if (!FileXInit.isTraditional) FileX.new("").usableSpace
                else FileX.new(destination).usableSpace
            }

        }
        catch (e: Exception){
            e.printStackTrace()
            return arrayOf(false, getStringFromRes(R.string.error_detecting_memory), e.message.toString())
        }

        Log.d(DEBUG_TAG, "Total size: $totalSize, availableBytes: $availableBytes")

        return if (totalSize > availableBytes){
            arrayOf(false, getStringFromRes(R.string.insufficient_storage),
                    "${getStringFromRes(R.string.estimated_files_size)} ${getHumanReadableStorageSpace(totalSize)}\n" +
                            "${getStringFromRes(R.string.available_space)} ${getHumanReadableStorageSpace(availableBytes)}\n\n" +
                            "${getStringFromRes(R.string.required_storage)} ${getHumanReadableStorageSpace(totalSize - availableBytes)}\n\n" +
                            getStringFromRes(R.string.will_be_compressed))
        }
        else {
            publishProgress(getStringFromRes(R.string.total_size_ok),
                    "${getStringFromRes(R.string.total_size)} ${getHumanReadableStorageSpace(totalSize)}\n" +
                            "${getStringFromRes(R.string.available_space)} ${getHumanReadableStorageSpace(availableBytes)}", "")

            Thread.sleep(1000)

            arrayOf(true)
        }
    }

    override suspend fun onProgressUpdate(vararg values: Any) {
        super.onProgressUpdate(*values)
        if (!cancelThis) {
            activity.updateWaitingLayout(*values)
        }
    }

    override suspend fun onPostExecute(result: Any?) {
        super.onPostExecute(result)

        try {

            result as Array<*>

            if (!(result[0] as Boolean)) {
                activity.onFinishReading(result)
            } else {
                activity.onFinishReading(arrayOf(true, appPackets))
            }
        }
        catch (e: Exception){
            e.printStackTrace()
            activity.onFinishReading(
                arrayOf(
                    false,
                    activity.getString(R.string.error_occurred),
                    "App size postExecute: ${e.message.toString()}"
                )
            )
        }
    }
}