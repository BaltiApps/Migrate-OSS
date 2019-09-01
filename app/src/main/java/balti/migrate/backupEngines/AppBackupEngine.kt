package balti.migrate.backupEngines

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageInfo
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.AsyncTask
import balti.migrate.R
import balti.migrate.extraBackupsActivity.apps.AppBatch
import balti.migrate.extraBackupsActivity.apps.AppPacket
import balti.migrate.utilities.CommonToolKotlin
import balti.migrate.utilities.CommonToolKotlin.Companion.ACTION_BACKUP_PROGRESS
import balti.migrate.utilities.CommonToolKotlin.Companion.EXTRA_APP_NAME
import balti.migrate.utilities.CommonToolKotlin.Companion.EXTRA_BACKUP_NAME
import balti.migrate.utilities.CommonToolKotlin.Companion.EXTRA_PART_NUMBER
import balti.migrate.utilities.CommonToolKotlin.Companion.EXTRA_PROGERSS_PERCENTAGE
import balti.migrate.utilities.CommonToolKotlin.Companion.EXTRA_PROGRESS_TYPE
import balti.migrate.utilities.CommonToolKotlin.Companion.EXTRA_PROGRESS_TYPE_MAKING_APP_SCRIPTS
import balti.migrate.utilities.CommonToolKotlin.Companion.EXTRA_TITLE
import balti.migrate.utilities.CommonToolKotlin.Companion.EXTRA_TOTAL_PARTS
import balti.migrate.utilities.CommonToolKotlin.Companion.MIGRATE_STATUS
import balti.migrate.utilities.CommonToolKotlin.Companion.MTD_APK
import balti.migrate.utilities.CommonToolKotlin.Companion.MTD_APP_ICON
import balti.migrate.utilities.CommonToolKotlin.Companion.MTD_APP_NAME
import balti.migrate.utilities.CommonToolKotlin.Companion.MTD_DATA
import balti.migrate.utilities.CommonToolKotlin.Companion.MTD_DATA_SIZE
import balti.migrate.utilities.CommonToolKotlin.Companion.MTD_ICON_FILE_NAME
import balti.migrate.utilities.CommonToolKotlin.Companion.MTD_IS_SYSTEM
import balti.migrate.utilities.CommonToolKotlin.Companion.MTD_PACKAGE_NAME
import balti.migrate.utilities.CommonToolKotlin.Companion.MTD_PERMISSION
import balti.migrate.utilities.CommonToolKotlin.Companion.MTD_SYSTEM_SIZE
import balti.migrate.utilities.CommonToolKotlin.Companion.MTD_VERSION
import balti.migrate.utilities.CommonToolKotlin.Companion.PREF_NEW_ICON_METHOD
import java.io.*
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

abstract class AppBackupEngine(private val bd: BackupIntentData,
                               private val appBatch: AppBatch,
                               private val doBackupInstallers : Boolean,
                               private val busyboxBinaryPath: String) : AsyncTask<Any, Any, Any>() {

    @Inject lateinit var engineContext: Context
    @Inject lateinit var sharedPrefs : SharedPreferences

    companion object {
        var ICON_STRING = ""
    }

    private var BACKUP_PID = -999
    private var isBackupCancelled = false

    private val backupDependencyComponent: BackupDependencyComponent
            by lazy { DaggerBackupDependencyComponent.create() }

    private val commonTools by lazy { CommonToolKotlin(engineContext) }
    private val actualBroadcast by lazy {
        Intent(ACTION_BACKUP_PROGRESS).apply {
            putExtra(EXTRA_BACKUP_NAME, bd.backupName)
            putExtra(EXTRA_PROGRESS_TYPE, "")
            putExtra(EXTRA_TOTAL_PARTS, bd.totalParts)
            putExtra(EXTRA_PART_NUMBER, bd.partNumber)
        }
    }
    private val pm by lazy { engineContext.packageManager }
    private val timeStamp by lazy { SimpleDateFormat("yyyy.MM.dd_HH.mm.ss").format(Calendar.getInstance().time);}

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

    private fun systemAppInstallScript(sysAppPackageName: String, sysAppPastingDir: String, appDir: String) {

        val scriptName = "$sysAppPackageName.sh"
        val scriptLocation = "${bd.destination}/${bd.backupName}/$scriptName"
        val script = File(scriptLocation)

        val scriptText = "#!sbin/sh\n\n" +
                "mkdir -p " + sysAppPastingDir + "\n" +
                "mv /tmp/" + appDir + "/*.apk " + sysAppPastingDir + "/" + "\n" +
                "cd /tmp/" + "\n" +
                "rm -rf " + appDir + "\n" +
                "rm -rf " + scriptName + "\n"


        File("${bd.destination}/${bd.backupName}").mkdirs()

        commonTools.tryIt {
            val writer = BufferedWriter(FileWriter(script))
            writer.write(scriptText)
            writer.close()
        }

        script.setExecutable(true, false)
    }

    private fun makeMetadataFile(isSystem: Boolean, appName: String, apkName: String, dataName: String, iconFileName: String?, version: String, permissions: Boolean, appPacket: AppPacket, iconString: String? = null) {

        val packageName = appPacket.PACKAGE_INFO.packageName

        val metadataFileName = "$packageName.json"
        val metadataFile = File("${bd.destination}/${bd.destination}/$metadataFileName")

        val metadataContent = "" +
                "{\n" +
                "   \"${MTD_IS_SYSTEM}\" : \"$isSystem\",\n" +
                "   \"${MTD_APP_NAME}\" : \"$appName\",\n" +
                "   \"${MTD_PACKAGE_NAME}\" : \"$packageName\",\n" +
                "   \"${MTD_APK}\" : \"$apkName\",\n" +
                "   \"${MTD_DATA}\" : \"$dataName\",\n" +
                "   \"${MTD_VERSION}\" : \"$version\",\n" +
                "   \"${MTD_DATA_SIZE}\" : ${appPacket.dataSize},\n" +
                "   \"${MTD_SYSTEM_SIZE}\" : ${appPacket.systemSize},\n" +
                "   \"${MTD_PERMISSION}\" : $permissions,\n" +
                when {
                    iconFileName != null -> "   \"${MTD_ICON_FILE_NAME}\" : \"$iconFileName\"\n"
                    iconString != null -> "   \"${MTD_APP_ICON}\" : $iconString\n"
                    else -> ""
                } +
                "}\n"

        File("${bd.destination}/${bd.backupName}").mkdirs()

        commonTools.tryIt {
            val writer = BufferedWriter(FileWriter(metadataFile))
            writer.write(metadataContent)
            writer.close()
        }
    }

    private fun makeIconFile(packageName: String, iconString: String): String{

        val iconFileName = "$packageName.icon"
        val iconFile = File("${bd.destination}/${bd.backupName}/$iconFileName")

        File("${bd.destination}/${bd.backupName}").mkdirs()

        commonTools.tryIt {
            val writer = BufferedWriter(FileWriter(iconFile))
            writer.write(iconString)
            writer.close()
        }

        return iconFileName
    }

    private fun makeBackupScript(): String{

        fun formatName(name: String): String{
            return name.replace(' ', '_')
                    .replace('`', '\'')
                    .replace('"', '\'')
        }

        val title = if (bd.totalParts > 1)
            engineContext.getString(R.string.making_app_script) + " : " + commonTools.getMadePartName(bd)
        else engineContext.getString(R.string.making_app_script)

        val scriptFile = File(engineContext.filesDir, "the_backup_script_${bd.partNumber}.sh")
        val scriptWriter = BufferedWriter(FileWriter(scriptFile))
        val appAndDataBackupScript = commonTools.unpackAssetToInternal("backup_app_and_data.sh", "backup_app_and_data.sh", false)

        scriptWriter.write("#!sbin/sh\n\n")
        scriptWriter.write("echo \" \"\n")
        scriptWriter.write("sleep 1\n")
        scriptWriter.write("echo \"--- PID: $$\"\n")
        scriptWriter.write("cp $scriptFile.absolutePath ${engineContext.externalCacheDir}/\n")
        scriptWriter.write("cp $busyboxBinaryPath ${bd.destination}/${bd.backupName}/\n")

        actualBroadcast.putExtra(EXTRA_PROGRESS_TYPE, EXTRA_PROGRESS_TYPE_MAKING_APP_SCRIPTS)
        actualBroadcast.putExtra(EXTRA_TITLE, title)

        appBatch.appPackets.let {
            for (i in 0 until it.size) {

                if (isBackupCancelled) break

                val packet = appBatch.appPackets[i]

                val appName = formatName(pm.getApplicationLabel(packet.PACKAGE_INFO.applicationInfo).toString())

                actualBroadcast.apply {
                    putExtra(EXTRA_PROGERSS_PERCENTAGE, commonTools.getPercentageText(i + 1, it.size))
                    putExtra(EXTRA_APP_NAME, appName)
                }
                commonTools.LBM?.sendBroadcast(actualBroadcast)

                val packageName = packet.PACKAGE_INFO.packageName
                var apkPath = packet.PACKAGE_INFO.applicationInfo.sourceDir
                var apkName = apkPath.substring(apkPath.lastIndexOf('/') + 1)       //has .apk extension
                apkPath = apkPath.substring(0, apkPath.lastIndexOf('/'))
                apkName = commonTools.applyNamingCorrectionForShell(apkName)

                var dataPath = "NULL"
                var dataName = "NULL"
                if (packet.DATA) {
                    dataPath = packet.PACKAGE_INFO.applicationInfo.dataDir
                    dataName = dataPath.substring(dataPath.lastIndexOf('/') + 1)
                    dataPath = dataPath.substring(0, dataPath.lastIndexOf('/'))
                }

                var versionName: String? = packet.PACKAGE_INFO.versionName
                versionName = if (versionName == null || versionName == "") "_"
                else formatName(versionName)

                val appIcon : String = getIconString(packet.PACKAGE_INFO)
                var appIconFileName : String? = null
                if (!sharedPrefs.getBoolean(PREF_NEW_ICON_METHOD, true))
                    appIconFileName = makeIconFile(packageName, appIcon)

                val echoCopyCommand = "echo \"$MIGRATE_STATUS: $appName (${(i + 1)}/${it.size}) icon: ${if (appIconFileName == null) appIcon else "$(cat $packageName.icon)"}\"\n"
                val scriptCommand = "sh " + appAndDataBackupScript + " " + packageName + " " + "${bd.destination}/${bd.backupName}" + " " +
                        apkPath + " " + apkName + " " + dataPath + " " + dataName + " " + busyboxBinaryPath + " " + it[i].PERMISSION + "\n"



                scriptWriter.write(echoCopyCommand, 0, echoCopyCommand.length)
                scriptWriter.write(scriptCommand, 0, scriptCommand.length)

                val isSystem = apkPath.startsWith("/system")
                if (isSystem)
                    systemAppInstallScript(packageName, apkPath, packageName)

                makeMetadataFile(isSystem, appName, apkName,
                        "$dataName.tar.gz", appIconFileName, versionName,
                        packet.PERMISSION, packet, if (appIconFileName != null) appIcon else null)
            }
        }

        scriptWriter.write("echo \"--- App files copied ---\"\n")
        scriptWriter.close()

        scriptFile.setExecutable(true)

        return scriptFile.absolutePath
    }

    override fun onPreExecute() {
        super.onPreExecute()
        backupDependencyComponent.inject(this)
    }

    override fun doInBackground(vararg params: Any?): Any {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onCancelled() {
        super.onCancelled()
        isBackupCancelled = true
    }
}