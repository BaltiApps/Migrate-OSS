package balti.migrate.backupEngines.engines

import android.widget.Toast
import balti.migrate.R
import balti.migrate.backupEngines.ParentBackupClass
import balti.migrate.backupEngines.containers.BackupIntentData
import balti.migrate.backupEngines.utils.BackupUtils
import balti.migrate.extraBackupsActivity.apps.containers.AppBatch
import balti.migrate.utilities.CommonToolKotlin.Companion.ERR_APP_BACKUP_SHELL
import balti.migrate.utilities.CommonToolKotlin.Companion.ERR_APP_BACKUP_SUPPRESSED
import balti.migrate.utilities.CommonToolKotlin.Companion.ERR_APP_BACKUP_TRY_CATCH
import balti.migrate.utilities.CommonToolKotlin.Companion.ERR_SCRIPT_MAKING_TRY_CATCH
import balti.migrate.utilities.CommonToolKotlin.Companion.EXTRA_APP_LOG
import balti.migrate.utilities.CommonToolKotlin.Companion.EXTRA_APP_NAME
import balti.migrate.utilities.CommonToolKotlin.Companion.EXTRA_PROGRESS_PERCENTAGE
import balti.migrate.utilities.CommonToolKotlin.Companion.EXTRA_PROGRESS_TYPE
import balti.migrate.utilities.CommonToolKotlin.Companion.EXTRA_PROGRESS_TYPE_APP_PROGRESS
import balti.migrate.utilities.CommonToolKotlin.Companion.EXTRA_PROGRESS_TYPE_MAKING_APP_SCRIPTS
import balti.migrate.utilities.CommonToolKotlin.Companion.EXTRA_SCRIPT_APP_NAME
import balti.migrate.utilities.CommonToolKotlin.Companion.EXTRA_TITLE
import balti.migrate.utilities.CommonToolKotlin.Companion.FILE_PREFIX_BACKUP_SCRIPT
import balti.migrate.utilities.CommonToolKotlin.Companion.FILE_PREFIX_RETRY_SCRIPT
import balti.migrate.utilities.CommonToolKotlin.Companion.MIGRATE_STATUS
import balti.migrate.utilities.CommonToolKotlin.Companion.PREF_NEW_ICON_METHOD
import java.io.*

abstract class AppBackupEngine(private val jobcode: Int, private val bd: BackupIntentData,
                               private val appBatch: AppBatch,
                               private val doBackupInstallers : Boolean,
                               private val busyboxBinaryPath: String) : ParentBackupClass(bd, "") {

    companion object {
        var ICON_STRING = ""
    }

    private var BACKUP_PID = -999

    private val pm by lazy { engineContext.packageManager }
    private val backupUtils by lazy { BackupUtils() }
    private var suProcess : Process? = null

    private val allErrors by lazy { ArrayList<String>(0) }
    private val actualErrors by lazy { ArrayList<String>(0) }

    private val fileListWriter by lazy { BufferedWriter(FileWriter(File(actualDestination, "fileList.txt"))) }

    init {

        customPreExecuteFunction = {
            if (bd.partNumber == 0){

                var previousBackupScripts = engineContext.filesDir.listFiles {
                    f -> (f.name.startsWith(FILE_PREFIX_BACKUP_SCRIPT) || f.name.startsWith(FILE_PREFIX_RETRY_SCRIPT)) &&
                        f.name.endsWith(".sh")
                }
                for (f in previousBackupScripts) f.delete()

                engineContext.externalCacheDir?.let {
                    previousBackupScripts = it.listFiles {
                        f -> (f.name.startsWith(FILE_PREFIX_BACKUP_SCRIPT) || f.name.startsWith(FILE_PREFIX_RETRY_SCRIPT)) &&
                            f.name.endsWith(".sh")
                    }
                    for (f in previousBackupScripts) f.delete()
                }
            }
        }

        customCancelFunction = { commonTools.tryIt { cancelTask() }}
    }

    private fun addToActualErrors(err: String){
        actualErrors.add(err)
        allErrors.add(err)
    }

    private fun systemAppInstallScript(sysAppPackageName: String, sysAppPastingDir: String, appDir: String) {

        val scriptName = "$sysAppPackageName.sh"
        val scriptLocation = "$actualDestination/$scriptName"
        val script = File(scriptLocation)

        var pastingDir = "/system"

        sysAppPastingDir.let {fullDir ->

            "/system/".let { if (fullDir.startsWith(it)) pastingDir = fullDir.substring(it.length) }

            "/system_root/".run {
                if (fullDir.startsWith(this)) {
                    pastingDir = fullDir.substring(this.length)

                    "/system_root/system/".run {
                        if (fullDir.startsWith(this)) {
                            pastingDir = fullDir.substring(this.length)

                            "/system_root/system/system".run {
                                if (fullDir.startsWith(this))
                                    pastingDir = fullDir.substring(this.length)
                            }

                        }

                    }
                }
            }
        }

        val scriptText = "#!sbin/sh\n\n" +
                "\n" +
                "SYSTEM=$(cat /tmp/migrate/SYSTEM)\n" +
                "mkdir -p \$SYSTEM/$pastingDir\n" +
                "mv /tmp/$appDir/*.apk \$SYSTEM/$pastingDir/\n" +
                "cd /tmp/" + "\n" +
                "rm -rf " + appDir + "\n" +
                "rm -rf " + scriptName + "\n"


        File(actualDestination).mkdirs()

        commonTools.tryIt {
            val writer = BufferedWriter(FileWriter(script))
            writer.write(scriptText)
            writer.close()
        }

        script.setExecutable(true, false)
    }

    private fun makeBackupScript(): String?{

        try {
            fun formatName(name: String): String {
                return name.replace(' ', '_')
                        .replace('`', '\'')
                        .replace('"', '\'')
            }

            val title = if (bd.totalParts > 1)
                engineContext.getString(R.string.making_app_script) + " : " + madePartName
            else engineContext.getString(R.string.making_app_script)

            val scriptFile = File(engineContext.filesDir, "$FILE_PREFIX_BACKUP_SCRIPT${bd.partNumber}.sh")
            val scriptWriter = BufferedWriter(FileWriter(scriptFile))
            val appAndDataBackupScript = commonTools.unpackAssetToInternal("backup_app_and_data.sh", "backup_app_and_data.sh", false)

            scriptWriter.write("#!sbin/sh\n\n")
            scriptWriter.write("echo \" \"\n")
            scriptWriter.write("sleep 1\n")
            scriptWriter.write("echo \"--- PID: $$\"\n")
            scriptWriter.write("cp $scriptFile.absolutePath ${engineContext.externalCacheDir}/\n")
            scriptWriter.write("cp $busyboxBinaryPath $actualDestination/\n")

            actualBroadcast.putExtra(EXTRA_PROGRESS_TYPE, EXTRA_PROGRESS_TYPE_MAKING_APP_SCRIPTS)
            actualBroadcast.putExtra(EXTRA_TITLE, title)

            appBatch.appPackets.let {packets ->
                for (i in 0 until packets.size) {

                    if (isBackupCancelled) break

                    val packet = packets[i]

                    val appName = formatName(pm.getApplicationLabel(packet.PACKAGE_INFO.applicationInfo).toString())

                    actualBroadcast.apply {
                        putExtra(EXTRA_PROGRESS_PERCENTAGE, commonTools.getPercentageText(i + 1, packets.size))
                        putExtra(EXTRA_SCRIPT_APP_NAME, appName)
                    }
                    commonTools.LBM?.sendBroadcast(actualBroadcast)

                    val packageName = packet.PACKAGE_INFO.packageName

                    var apkPath = "NULL"
                    var apkName = "NULL"       //has .apk extension
                    if (packet.APP) {

                        apkPath = packet.PACKAGE_INFO.applicationInfo.sourceDir
                        apkName = apkPath.substring(apkPath.lastIndexOf('/') + 1)
                        apkPath = apkPath.substring(0, apkPath.lastIndexOf('/'))
                        apkName = commonTools.applyNamingCorrectionForShell(apkName)

                        fileListWriter.write("$packageName.app\n")
                    }

                    var dataPath = "NULL"
                    var dataName = "NULL"
                    if (packet.DATA) {
                        dataPath = packet.PACKAGE_INFO.applicationInfo.dataDir
                        dataName = dataPath.substring(dataPath.lastIndexOf('/') + 1)
                        dataPath = dataPath.substring(0, dataPath.lastIndexOf('/'))

                        fileListWriter.write("$packageName.tar.gz\n")
                    }

                    if (packet.PERMISSION) {
                        backupUtils.makePermissionFile(packageName, actualDestination, pm)
                        fileListWriter.write("$packageName.perm\n")
                    }

                    var versionName: String? = packet.PACKAGE_INFO.versionName
                    versionName = if (versionName == null || versionName == "") "_"
                    else formatName(versionName)

                    val appIcon: String = backupUtils.getIconString(packet.PACKAGE_INFO, pm)
                    var appIconFileName: String? = null
                    if (!sharedPreferences.getBoolean(PREF_NEW_ICON_METHOD, true)) {
                        appIconFileName = backupUtils.makeIconFile(packageName, appIcon, actualDestination)
                        fileListWriter.write("$packageName.icon\n")
                    }

                    val echoCopyCommand = "echo \"$MIGRATE_STATUS: $appName (${(i + 1)}/${packets.size}) icon: ${if (appIconFileName == null) appIcon else "$(cat $packageName.icon)"}\"\n"
                    val scriptCommand = "sh $appAndDataBackupScript " +
                            "$packageName $actualDestination " +
                            "$apkPath $apkName " +
                            "$dataPath $dataName " +
                            "$busyboxBinaryPath\n"

                    scriptWriter.write(echoCopyCommand, 0, echoCopyCommand.length)
                    scriptWriter.write(scriptCommand, 0, scriptCommand.length)

                    val isSystem = apkPath.startsWith("/system")
                    if (isSystem) systemAppInstallScript(packageName, apkPath, packageName)

                    backupUtils.makeMetadataFile(
                            isSystem, appName, apkName, "$dataName.tar.gz", appIconFileName,
                            versionName, packet.PERMISSION, packet, bd, doBackupInstallers, actualDestination,
                            if (appIconFileName != null) appIcon else null
                    )
                }

            }

            scriptWriter.write("echo \"--- App files copied ---\"\n")
            scriptWriter.close()

            scriptFile.setExecutable(true)

            fileListWriter.close()

            return scriptFile.absolutePath
        }
        catch (e: Exception){
            e.printStackTrace()
            addToActualErrors("$ERR_SCRIPT_MAKING_TRY_CATCH${bd.errorTag}: ${e.message}")
            return null
        }
    }

    private fun runBackupScript(scriptFileLocation: String){

        try {

            if (!File(scriptFileLocation).exists())
                throw Exception(engineContext.getString(R.string.script_file_does_not_exist))

            suProcess = Runtime.getRuntime().exec("su")
            suProcess?.let {
                val suInputStream = BufferedWriter(OutputStreamWriter(it.outputStream))
                val outputStream = BufferedReader(InputStreamReader(it.inputStream))
                val errorStream = BufferedReader(InputStreamReader(it.errorStream))

                suInputStream.write("sh $scriptFileLocation\n")
                suInputStream.write("exit\n")
                suInputStream.flush()

                var c = 0

                backupUtils.iterateBufferedReader(outputStream, { output ->

                    if (isBackupCancelled) return@iterateBufferedReader true

                    actualBroadcast.putExtra(EXTRA_PROGRESS_TYPE, EXTRA_PROGRESS_TYPE_APP_PROGRESS)

                    if (output.startsWith("--- PID:")) {
                        commonTools.tryIt {
                            BACKUP_PID = output.substring(output.lastIndexOf(" ") + 1).toInt()
                        }
                    }

                    var line = ""

                    if (output.startsWith(MIGRATE_STATUS)) {

                        line = output.substring(MIGRATE_STATUS.length + 2)

                        if (line.contains("icon:")) {
                            ICON_STRING = line.substring(line.lastIndexOf(' ')).trim()
                            line = line.substring(0, line.indexOf("icon:"))
                        }

                        val title = if (bd.totalParts > 1)
                            engineContext.getString(R.string.backingUp) + " : " + madePartName
                        else engineContext.getString(R.string.backingUp)

                        actualBroadcast.putExtra(EXTRA_APP_NAME, line)
                        actualBroadcast.putExtra(EXTRA_PROGRESS_PERCENTAGE, commonTools.getPercentage(++c, appBatch.appPackets.size))
                        actualBroadcast.putExtra(EXTRA_TITLE, title)

                        commonTools.LBM?.sendBroadcast(actualBroadcast)
                    }

                    actualBroadcast.putExtra(EXTRA_APP_LOG, line)
                    commonTools.LBM?.sendBroadcast(actualBroadcast)

                    return@iterateBufferedReader line == "--- App files copied ---"
                })

                commonTools.tryIt { it.waitFor() }

                backupUtils.iterateBufferedReader(errorStream, { errorLine ->

                    var ignorable = false

                    (BackupUtils.ignorableWarnings + BackupUtils.correctableErrors).forEach {warnings ->
                        if (errorLine.endsWith(warnings)) ignorable = true
                    }

                    if (!ignorable)
                        addToActualErrors("$ERR_APP_BACKUP_SHELL${bd.errorTag}: $errorLine")
                    else allErrors.add("$ERR_APP_BACKUP_SUPPRESSED${bd.errorTag}: $errorLine")

                    return@iterateBufferedReader false
                })

            }
        }
        catch (e: Exception){
            e.printStackTrace()
            addToActualErrors("$ERR_APP_BACKUP_TRY_CATCH${bd.errorTag}: ${e.message}")
        }
    }

    private fun cancelTask() {
        if (BACKUP_PID != -999) {
            commonTools.tryIt {
                val killProcess = Runtime.getRuntime().exec("su")

                val writer = BufferedWriter(OutputStreamWriter(killProcess.outputStream))
                writer.write("kill -9 $BACKUP_PID\n")
                writer.write("kill -15 $BACKUP_PID\n")
                writer.write("exit\n")
                writer.flush()

                commonTools.tryIt { killProcess.waitFor() }
                commonTools.tryIt { suProcess?.waitFor() }

                Toast.makeText(engineContext, engineContext.getString(R.string.deletingFiles), Toast.LENGTH_SHORT).show()

                if (bd.totalParts > 1) {
                    commonTools.dirDelete(actualDestination)
                    commonTools.dirDelete("$actualDestination.zip")
                } else {
                    commonTools.dirDelete(bd.destination)
                }
            }
        }
    }

    override fun doInBackground(vararg params: Any?): Any {
        val scriptLocation = makeBackupScript()
        scriptLocation?.let { runBackupScript(it) }
        return 0
    }

    override fun onPostExecute(result: Any?) {
        super.onPostExecute(result)
        BACKUP_PID = -999
        if (actualErrors.size == 0)
            onBackupComplete.onBackupComplete(jobcode, true, allErrors)
        else onBackupComplete.onBackupComplete(jobcode, false, allErrors)
    }
}