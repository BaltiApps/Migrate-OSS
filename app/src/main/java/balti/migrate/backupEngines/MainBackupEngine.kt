package balti.migrate.backupEngines

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.AsyncTask
import android.os.Build
import android.support.v4.app.NotificationCompat
import android.widget.Toast
import balti.migrate.R
import balti.migrate.extraBackupsActivity.apps.AppBatch
import balti.migrate.extraBackupsActivity.apps.AppPacket
import balti.migrate.extraBackupsActivity.calls.CallsDataPacketsKotlin
import balti.migrate.extraBackupsActivity.contacts.ContactsDataPacketKotlin
import balti.migrate.extraBackupsActivity.sms.SmsDataPacketKotlin
import balti.migrate.utilities.CommonToolKotlin
import balti.migrate.utilities.CommonToolKotlin.Companion.ACTION_BACKUP_PROGRESS
import balti.migrate.utilities.CommonToolKotlin.Companion.BACKUP_NAME_SETTINGS
import balti.migrate.utilities.CommonToolKotlin.Companion.BACKUP_NAME_WIFI
import balti.migrate.utilities.CommonToolKotlin.Companion.CHANNEL_BACKUP_RUNNING
import balti.migrate.utilities.CommonToolKotlin.Companion.EXTRA_BACKUP_NAME
import balti.migrate.utilities.CommonToolKotlin.Companion.EXTRA_ERRORS
import balti.migrate.utilities.CommonToolKotlin.Companion.EXTRA_PART_NAME
import balti.migrate.utilities.CommonToolKotlin.Companion.EXTRA_PART_NUMBER
import balti.migrate.utilities.CommonToolKotlin.Companion.EXTRA_PROGRESS_TYPE
import balti.migrate.utilities.CommonToolKotlin.Companion.EXTRA_PROGRESS_TYPE_TESTING
import balti.migrate.utilities.CommonToolKotlin.Companion.EXTRA_TEST_LOG
import balti.migrate.utilities.CommonToolKotlin.Companion.EXTRA_TOTAL_PARTS
import balti.migrate.utilities.CommonToolKotlin.Companion.MTD_APP_NAME
import balti.migrate.utilities.CommonToolKotlin.Companion.MTD_DATA
import balti.migrate.utilities.CommonToolKotlin.Companion.MTD_DATA_SIZE
import balti.migrate.utilities.CommonToolKotlin.Companion.MTD_ICON_FILE_NAME
import balti.migrate.utilities.CommonToolKotlin.Companion.MTD_PACKAGE_NAME
import balti.migrate.utilities.CommonToolKotlin.Companion.MTD_PERMISSION
import balti.migrate.utilities.CommonToolKotlin.Companion.MTD_SYSTEM_SIZE
import balti.migrate.utilities.CommonToolKotlin.Companion.MTD_VERSION
import java.io.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class MainBackupEngine(private var context: Context, private var backupName: String, private var destination: String,
                       private var compressionLevel: Int, private var partNumber: Int, private var totalParts: Int,
                       private var contactList : ArrayList<ContactsDataPacketKotlin>?, private var doBackupContacts : Boolean,
                       private var callsList: ArrayList<CallsDataPacketsKotlin>?, private var doBackupCalls : Boolean,
                       private var smsList: ArrayList<SmsDataPacketKotlin>?, private var doBackupSms : Boolean,
                       private var dpiText: String, private var doBackupDpi : Boolean,
                       private var keyboardText: String, private var doBackupKeyboard : Boolean,
                       private var adbState: Int, private var doBackupAdb : Boolean,
                       private var fontScale: Double, private var doBackupFontScale : Boolean,
                       private var wifiContent: ArrayList<String>, private var doBackupWifi : Boolean,
                       private var appBatch: AppBatch, private var doBackupInstallers : Boolean) : AsyncTask<Any, Any, Any>() {

    private val commonTools by lazy {
        CommonToolKotlin(context)
    }

    private val madePartName by lazy {
        (totalParts > 1).let {
            if (it) "${context.getString(R.string.part)} $partNumber ${context.getString(R.string.of)} $totalParts"
            else ""
        }
    }

    private val errorTag by lazy { "[$partNumber/$totalParts]" }

    private val busyboxBinaryPath by lazy {
        Build.SUPPORTED_ABIS[0].let {
            if (it == "armeabi-v7a" || it == "arm64-v8a")
                commonTools.unpackAssetToInternal("busybox", "busybox", true)
            else if (it == "x86" || it == "x86_64")
                commonTools.unpackAssetToInternal("busybox-x86", "busybox", true)
            else ""
        }
    }

    private val timeInMillis: Long
        get() = Calendar.getInstance().timeInMillis

    private val pm by lazy { context.packageManager }
    private val timeStamp by lazy { SimpleDateFormat("yyyy.MM.dd_HH.mm.ss").format(Calendar.getInstance().time);}

    private var suProcess : Process? = null
    private var suWriter : BufferedWriter? = null

    private var finalMessage = ""
    private var errors = ArrayList<String>(0)

    private val contactsBackupName by lazy { "Contacts_$timeStamp.vcf" }
    private val smsBackupName by lazy { "Sms_$timeStamp.vcf" }
    private val callsBackupName by lazy { "Calls_$timeStamp.vcf" }
    private val settingsBackupName by lazy { BACKUP_NAME_SETTINGS }
    private val wifiBackupName by lazy { BACKUP_NAME_WIFI }

    private val actualProgressBroadcast by lazy { Intent(ACTION_BACKUP_PROGRESS).apply {
        putExtra(EXTRA_PART_NAME, madePartName)
        putExtra(EXTRA_BACKUP_NAME, backupName)
        putExtra(EXTRA_TOTAL_PARTS, totalParts)
        putExtra(EXTRA_PART_NUMBER, partNumber)
    } }

    private var isBackupCancelled = false
    private var startMillis = 0L
    private var endMillis = 0L

    private var BACKUP_PID = -999
    private var CORRECTING_PID = -999
    private var TESTING_PID = -999

    companion object {
        var ICON_STRING = ""
    }

    private fun createNotificationBuilder() =
            NotificationCompat.Builder(context, CHANNEL_BACKUP_RUNNING)

    private fun iterateBufferedReader(reader: BufferedReader, loopFunction: (line: String) -> Boolean,
                                      onCancelledFunction: (() -> Unit)? = null, isMasterCancelApplicable: Boolean = true){
        var doBreak = false
        while (true){
            val line : String? = reader.readLine()
            if (line == null) break
            else {
                if (!isMasterCancelApplicable || !isBackupCancelled) {
                    doBreak = loopFunction(line.trim())
                    if (doBreak) break
                }
                else break
            }
        }
        if (isBackupCancelled || doBreak) onCancelledFunction?.invoke()
    }

    private fun moveFile(source: File, destination: File): String{
        try {
            val bufferedInputStream = BufferedInputStream(FileInputStream(source))
            val fileOutputStream = FileOutputStream(destination)

            val buffer = ByteArray(4096)
            var read: Int

            while (true){
                read = bufferedInputStream.read(buffer)
                if (read > 0) fileOutputStream.write(buffer, 0, read)
                else break
            }
            return ""
        }
        catch (e: Exception){
            e.printStackTrace()
            return e.message.toString()
        }
    }

    private fun getIconString(packageInfo: PackageInfo): String {

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

    private fun getAllFiles(directory: File, allFiles: ArrayList<File>): ArrayList<File>{
        if (!directory.isDirectory) return arrayListOf(directory)
        else {
            for (f in directory.listFiles()){
                allFiles.add(f)
                if (f.isDirectory) getAllFiles(f, allFiles)
            }
        }
        return allFiles
    }

    private fun systemAppInstallScript(sysAppPackageName: String, sysAppPastingDir: String, appDir: String) {

        val scriptName = "$sysAppPackageName.sh"
        val scriptLocation = "$destination/$backupName/$scriptName"
        val script = File(scriptLocation)

        val scriptText = "#!sbin/sh\n\n" +
                "mkdir -p " + sysAppPastingDir + "\n" +
                "mv /tmp/" + appDir + "/*.apk " + sysAppPastingDir + "/" + "\n" +
                "cd /tmp/" + "\n" +
                "rm -rf " + appDir + "\n" +
                "rm -rf " + scriptName + "\n"


        File("$destination/$backupName").mkdirs()

        commonTools.tryIt {
            val writer = BufferedWriter(FileWriter(script))
            writer.write(scriptText)
            writer.close()
        }

        script.setExecutable(true, false)
    }

    private fun makePackageData() {

        val packageData = File("$destination/$backupName/package-data.txt")
        var contents = ""

        packageData.parentFile.mkdirs()

        contents += "backup_name $backupName\n"
        contents += "timestamp $timeStamp\n"
        contents += "device " + Build.DEVICE + "\n"
        contents += "sdk " + Build.VERSION.SDK_INT + "\n"
        contents += "cpu_abi " + Build.SUPPORTED_ABIS[0] + "\n"
        contents += "data_required_size ${appBatch.dataSize}\n"
        contents += "system_required_size ${appBatch.systemSize}\n"
        contents += "migrate_version " + context.getString(R.string.current_version_name) + "\n"

        commonTools.tryIt {
            val writer = BufferedWriter(FileWriter(packageData))
            writer.write(contents)
            writer.close()
        }
    }

    private fun makeExtrasData() {

        val package_data = File("$destination/$backupName/extras-data")
        var contents = ""

        package_data.parentFile.mkdirs()

        if (doBackupContacts) contents += contactsBackupName + "\n"
        if (doBackupSms) contents += smsBackupName + "\n"
        if (doBackupCalls) contents += callsBackupName + "\n"
        if (doBackupDpi || doBackupAdb || doBackupFontScale || doBackupKeyboard) {
            contents += settingsBackupName + "\n"
        }
        if (doBackupWifi) contents += wifiBackupName + "\n"

        commonTools.tryIt {
            val writer = BufferedWriter(FileWriter(package_data))
            writer.write(contents)
            writer.close()
        }
    }

    private fun makeMetadataFile(appName: String, apkName: String, dataName: String, iconFileName: String, version: String, permissions: Boolean, appPacket: AppPacket) {

        val packageName = appPacket.PACKAGE_INFO.packageName

        val metadataFileName = "$packageName.json"
        val metadataFile = File("$destination/$backupName/$metadataFileName")

        val metadataContent = "" +
                "{\n" +
                "   \"$MTD_APP_NAME\" : \"$appName\",\n" +
                "   \"$MTD_PACKAGE_NAME\" : \"$packageName\",\n" +
                "   \"$MTD_APP_NAME\" : \"$apkName\",\n" +
                "   \"$MTD_DATA\" : \"$dataName\",\n" +
                "   \"$MTD_ICON_FILE_NAME\" : \"$iconFileName\",\n" +
                "   \"$MTD_VERSION\" : \"$version\",\n" +
                "   \"$MTD_DATA_SIZE\" : ${appPacket.dataSize},\n" +
                "   \"$MTD_SYSTEM_SIZE\" : ${appPacket.systemSize},\n" +
                "   \"$MTD_PERMISSION\" : $permissions\n" +
                "}\n"

        File("$destination/$backupName").mkdirs()

        commonTools.tryIt {
            val writer = BufferedWriter(FileWriter(metadataFile))
            writer.write(metadataContent)
            writer.close()
        }
    }

    private fun makeIconFile(packageName: String, iconString: String){

        val iconFileName = "$packageName.icon"
        val iconFile = File("$destination/$backupName/$iconFileName")

        File("$destination/$backupName").mkdirs()

        commonTools.tryIt {
            val writer = BufferedWriter(FileWriter(iconFile))
            writer.write(iconString)
            writer.close()
        }
    }

    fun cancelTask() {
        commonTools.tryIt {
            val killProcess = Runtime.getRuntime().exec("su")
            val writer = BufferedWriter(OutputStreamWriter(killProcess.outputStream))
            fun killPID(pid: Int){
                if (pid != -999){
                    writer.write("kill -9 $pid\n")
                    writer.write("kill -15 $pid\n")
                }
            }
            killPID(TESTING_PID)
            killPID(BACKUP_PID)
            killPID(CORRECTING_PID)
            writer.flush()

            killProcess.waitFor()

            commonTools.tryIt { suProcess?.waitFor() }

            isBackupCancelled = true

            Toast.makeText(context, context.getString(R.string.deletingFiles), Toast.LENGTH_SHORT).show()

            if (totalParts > 1) {
                commonTools.dirDelete("$destination/$backupName")
                commonTools.dirDelete("$destination/$backupName.zip")
            } else {
                commonTools.dirDelete(destination)
            }

            cancel(true)
        }
    }

    private fun testSystemAbility(): ArrayList<String>{

        val testingErrors = ArrayList<String>(0)

        try {
            val testScriptPath = commonTools.unpackAssetToInternal("systemTestScript.sh", "test.sh", false)

            val thisPackageInfo = pm.getApplicationInfo(context.packageName, 0)

            val thisDataPath = thisPackageInfo.dataDir.let {
                if (it.endsWith("/")) it.substring(0, it.length-1)
                else ""
            }

            val suProcess = Runtime.getRuntime().exec("su")
            val suWriter = BufferedWriter(OutputStreamWriter(suProcess.outputStream))
            suWriter.run {
                this.write("sh $testScriptPath ${thisPackageInfo.packageName} ${thisPackageInfo.sourceDir} $thisDataPath ${context.externalCacheDir} $busyboxBinaryPath\n")
                this.write("exit\n")
                this.flush()
            }

            val resultStream = BufferedReader(InputStreamReader(suProcess.inputStream))

            iterateBufferedReader(resultStream, {line ->

                actualProgressBroadcast.putExtra(EXTRA_PROGRESS_TYPE, EXTRA_PROGRESS_TYPE_TESTING)
                if (line.startsWith("--- PID:")){
                    commonTools.tryIt { TESTING_PID = line.substring(line.lastIndexOf(" ") + 1).toInt() }
                }

                actualProgressBroadcast.putExtra(EXTRA_TEST_LOG, line)
                commonTools.LBM?.sendBroadcast(actualProgressBroadcast)

                return@iterateBufferedReader line == "--- Test done ---"
            })

            val errorStream = BufferedReader(InputStreamReader(suProcess.errorStream))

            iterateBufferedReader(errorStream, {line ->
                testingErrors.add(line)
                return@iterateBufferedReader false
            }, null, false)

            val expectedApkFile = File(context.externalCacheDir, "${thisPackageInfo.packageName}.apk")
            val expectedDataFile = File(context.externalCacheDir, "${thisPackageInfo.packageName}.tar.gz")

            if (!expectedApkFile.exists() || expectedApkFile.length() == 0L)
                testingErrors.add(context.getString(R.string.test_apk_not_found))
            if (!expectedDataFile.exists() || expectedDataFile.length() == 0L)
                testingErrors.add(context.getString(R.string.test_data_not_found))
        }
        catch (e: Exception){
            e.printStackTrace()
            testingErrors.add(e.message.toString())
        }

        if (testingErrors.size > 0) {
            actualProgressBroadcast.putExtra(EXTRA_PROGRESS_TYPE, EXTRA_PROGRESS_TYPE_TESTING)
            actualProgressBroadcast.putStringArrayListExtra(EXTRA_ERRORS, testingErrors)
            commonTools.LBM?.sendBroadcast(actualProgressBroadcast)
        }

        return testingErrors
    }

    override fun onPreExecute() {
        super.onPreExecute()
        if (totalParts > 1){
            destination = "$destination/$backupName"
            backupName = backupName.replace(" ", "_")
        }
        if (backupName.endsWith(".zip")) backupName = backupName.substring(0, backupName.lastIndexOf("."))
    }

    override fun doInBackground(vararg params: Any?): Any {

        return 0
    }
}