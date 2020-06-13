package balti.migrate.backupEngines.engines

import balti.migrate.R
import balti.migrate.backupEngines.BackupServiceKotlin
import balti.migrate.backupEngines.ParentBackupClass
import balti.migrate.backupEngines.containers.BackupIntentData
import balti.migrate.backupEngines.utils.BackupUtils
import balti.migrate.extraBackupsActivity.apps.containers.AppPacket
import balti.migrate.utilities.CommonToolsKotlin.Companion.ERR_APP_BACKUP_SHELL
import balti.migrate.utilities.CommonToolsKotlin.Companion.ERR_APP_BACKUP_SUPPRESSED
import balti.migrate.utilities.CommonToolsKotlin.Companion.ERR_APP_BACKUP_TRY_CATCH
import balti.migrate.utilities.CommonToolsKotlin.Companion.ERR_SCRIPT_MAKING_TRY_CATCH
import balti.migrate.utilities.CommonToolsKotlin.Companion.EXTRA_PROGRESS_TYPE_APP_PROGRESS
import balti.migrate.utilities.CommonToolsKotlin.Companion.EXTRA_PROGRESS_TYPE_MAKING_APP_SCRIPTS
import balti.migrate.utilities.CommonToolsKotlin.Companion.FILE_PREFIX_BACKUP_SCRIPT
import balti.migrate.utilities.CommonToolsKotlin.Companion.FILE_PREFIX_RETRY_SCRIPT
import balti.migrate.utilities.CommonToolsKotlin.Companion.MIGRATE_STATUS
import balti.migrate.utilities.CommonToolsKotlin.Companion.PREF_IGNORE_APP_CACHE
import balti.migrate.utilities.CommonToolsKotlin.Companion.PREF_NEW_ICON_METHOD
import balti.migrate.utilities.IconTools
import balti.module.baltitoolbox.functions.FileHandlers.unpackAssetToInternal
import balti.module.baltitoolbox.functions.Misc.getPercentage
import balti.module.baltitoolbox.functions.Misc.tryIt
import balti.module.baltitoolbox.functions.SharedPrefs.getPrefBoolean
import java.io.*

class AppBackupEngine(private val jobcode: Int, private val bd: BackupIntentData,
                               private val appList: ArrayList<AppPacket>,
                               private val doBackupInstallers : Boolean,
                               private val busyboxBinaryPath: String) : ParentBackupClass(bd, "") {

    companion object {
        var ICON_STRING = ""
    }

    private var BACKUP_PID = -999

    private val pm by lazy { engineContext.packageManager }
    private val backupUtils by lazy { BackupUtils() }
    private val iconTools by lazy { IconTools() }
    private var suProcess : Process? = null

    private val allErrors by lazy { ArrayList<String>(0) }
    private val actualErrors by lazy { ArrayList<String>(0) }

    init {
        customPreExecuteFunction = {

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

    private fun addToActualErrors(err: String){
        actualErrors.add(err)
        allErrors.add(err)
    }

    private fun systemAppInstallScript(packageName: String, apkPath: String) {

        val scriptName = "$packageName.sh"
        val scriptLocation = "$actualDestination/$scriptName"
        val script = File(scriptLocation)

        val pastingDir: String = if (apkPath.contains("priv-app"))
            apkPath.substring(apkPath.indexOf("/priv-app"))
        else apkPath.substring(apkPath.indexOf("/app"))

        val scriptText = "#!sbin/sh\n\n" +
                "\n" +
                "SYSTEM=$(cat /tmp/migrate/SYSTEM)\n" +
                "mkdir -p \$SYSTEM/$pastingDir\n" +
                "mv /tmp/$packageName/*.apk \$SYSTEM/$pastingDir/\n" +
                "rm -rf /tmp/$packageName\n" +
                "rm /tmp/$scriptName\n"


        File(actualDestination).mkdirs()

        tryIt {
            val writer = BufferedWriter(FileWriter(script))
            writer.write(scriptText)
            writer.close()
        }

        script.setExecutable(true, false)
    }

    private fun makeBackupScript(): String?{

        try {

            val title = getTitle(R.string.making_app_script)

            resetBroadcast(false, title, EXTRA_PROGRESS_TYPE_MAKING_APP_SCRIPTS)

            val ignoreCache = getPrefBoolean(PREF_IGNORE_APP_CACHE, false)

            val scriptFile = File(engineContext.filesDir, "$FILE_PREFIX_BACKUP_SCRIPT.sh")
            scriptFile.parentFile?.mkdirs()
            val scriptWriter = BufferedWriter(FileWriter(scriptFile))
            //val appAndDataBackupScript = commonTools.unpackAssetToInternal("backup_app_and_data.sh", "backup_app_and_data.sh", false)
            val appAndDataBackupScript = unpackAssetToInternal("backup_app_and_data.sh")

            scriptWriter.write("#!sbin/sh\n\n")
            scriptWriter.write("echo \" \"\n")
            scriptWriter.write("sleep 1\n")
            scriptWriter.write("echo \"--- PID: $$\"\n")
            scriptWriter.write("cp ${scriptFile.absolutePath} ${engineContext.externalCacheDir}/\n")

            appList.let {packets ->
                for (i in 0 until packets.size) {

                    if (BackupServiceKotlin.cancelAll) break

                    val packet = packets[i]

                    val modifiedAppName = "${packet.appName}(${i+1}/${packets.size})"
                    val packageName = packet.PACKAGE_INFO.packageName

                    broadcastProgress(modifiedAppName, modifiedAppName, true, getPercentage(i + 1, packets.size))

                    if (packet.PERMISSION) {
                        backupUtils.makePermissionFile(packageName, actualDestination, pm)
                    }

                    var versionName: String? = packet.PACKAGE_INFO.versionName
                    versionName = if (versionName == null || versionName == "") "_"
                    else formatName(versionName)

                    var appIconFileName: String? = null
                    if (getPrefBoolean(PREF_NEW_ICON_METHOD, true)) {
                        appIconFileName = backupUtils.makeNewIconFile(packageName, iconTools.getBitmap(packet.PACKAGE_INFO, pm), actualDestination)
                    }
                    else {
                        val appIcon: String = iconTools.getIconString(packet.PACKAGE_INFO, pm)
                        appIconFileName = backupUtils.makeStringIconFile(packageName, appIcon, actualDestination)
                    }

                    val echoCopyCommand = "echo \"$MIGRATE_STATUS: $modifiedAppName icon: $appIconFileName\"\n"
                    val scriptCommand = "sh $appAndDataBackupScript " +
                            "$packageName $actualDestination " +
                            "${packet.apkPath} ${packet.apkName} " +
                            "${packet.dataPath} ${packet.dataName} " +
                            "$busyboxBinaryPath $ignoreCache\n"

                    scriptWriter.write(echoCopyCommand, 0, echoCopyCommand.length)
                    scriptWriter.write(scriptCommand, 0, scriptCommand.length)

                    tryIt { if (packet.isSystem) systemAppInstallScript(packageName, packet.apkPath) }

                    backupUtils.makeMetadataFile(versionName, appIconFileName, null, packet, bd, doBackupInstallers)
                }

            }

            scriptWriter.write("echo \"--- App files copied ---\"\n")
            scriptWriter.close()

            scriptFile.setExecutable(true)

            return scriptFile.absolutePath
        }
        catch (e: Exception){
            e.printStackTrace()
            addToActualErrors("$ERR_SCRIPT_MAKING_TRY_CATCH: ${e.message}")
            return null
        }
    }

    private fun runBackupScript(scriptFileLocation: String){

        try {

            if (!File(scriptFileLocation).exists())
                throw Exception(engineContext.getString(R.string.script_file_does_not_exist))

            val title = getTitle(R.string.backingUp)

            resetBroadcast(false, title, EXTRA_PROGRESS_TYPE_APP_PROGRESS)

            suProcess = Runtime.getRuntime().exec("su")
            suProcess?.let {
                val suInputStream = BufferedWriter(OutputStreamWriter(it.outputStream))
                val outputStream = BufferedReader(InputStreamReader(it.inputStream))
                val errorStream = BufferedReader(InputStreamReader(it.errorStream))

                suInputStream.write("sh $scriptFileLocation\n")
                suInputStream.write("exit\n")
                suInputStream.flush()

                var c = 0
                var appName = ""
                var progress = 0

                backupUtils.iterateBufferedReader(outputStream, { output ->

                    if (BackupServiceKotlin.cancelAll) {
                        cancelTask(suProcess, BACKUP_PID)
                        return@iterateBufferedReader true
                    }

                    if (output.startsWith("--- PID:")) {
                        tryIt {
                            BACKUP_PID = output.substring(output.lastIndexOf(" ") + 1).toInt()
                        }
                    }

                    var line = output

                    if (output.startsWith(MIGRATE_STATUS)) {

                        line = output.substring(MIGRATE_STATUS.length + 2)

                        if (line.contains("icon:")) {
                            ICON_STRING = line.substring(line.lastIndexOf(' ')).trim()
                            line = line.substring(0, line.indexOf("icon:"))
                        }

                        appName = line
                        progress = getPercentage(++c, appList.size)
                        broadcastProgress(appName, "\n${appName}", true, progress)
                    }
                    else broadcastProgress(appName, line, false)

                    return@iterateBufferedReader line == "--- App files copied ---"
                })

                tryIt { it.waitFor() }

                backupUtils.iterateBufferedReader(errorStream, { errorLine ->

                    var ignorable = false

                    (BackupUtils.ignorableWarnings + BackupUtils.correctableErrors).forEach { warnings ->
                        if (errorLine.endsWith(warnings)) ignorable = true
                    }

                    if (!ignorable)
                        addToActualErrors("$ERR_APP_BACKUP_SHELL: $errorLine")
                    else allErrors.add("$ERR_APP_BACKUP_SUPPRESSED: $errorLine")

                    return@iterateBufferedReader false
                })

            }

        }
        catch (e: Exception){
            e.printStackTrace()
            addToActualErrors("$ERR_APP_BACKUP_TRY_CATCH: ${e.message}")
        }
    }

    override suspend fun doInBackground(arg: Any?): Any? {
        val scriptLocation = makeBackupScript()
        if (!BackupServiceKotlin.cancelAll) scriptLocation?.let { runBackupScript(it) }
        return 0
    }

    override fun postExecuteFunction() {
        BACKUP_PID = -999
        onEngineTaskComplete.onComplete(jobcode, actualErrors, jobResults = allErrors)
    }
}