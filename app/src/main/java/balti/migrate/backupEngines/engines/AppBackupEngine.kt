package balti.migrate.backupEngines.engines

import balti.filex.FileX
import balti.migrate.AppInstance.Companion.CACHE_DIR
import balti.migrate.AppInstance.Companion.appApkFiles
import balti.migrate.AppInstance.Companion.appPackets
import balti.migrate.R
import balti.migrate.backupEngines.BackupServiceKotlin
import balti.migrate.backupEngines.ParentBackupClass_new
import balti.migrate.backupEngines.utils.BackupUtils
import balti.migrate.utilities.BackupProgressNotificationSystem.Companion.ProgressType.*
import balti.migrate.utilities.CommonToolsKotlin
import balti.migrate.utilities.CommonToolsKotlin.Companion.ERR_APP_BACKUP_SHELL
import balti.migrate.utilities.CommonToolsKotlin.Companion.ERR_APP_BACKUP_SUPPRESSED
import balti.migrate.utilities.CommonToolsKotlin.Companion.ERR_APP_BACKUP_TRY_CATCH
import balti.migrate.utilities.CommonToolsKotlin.Companion.ERR_AUX_MOVING_SU
import balti.migrate.utilities.CommonToolsKotlin.Companion.ERR_AUX_MOVING_TRY_CATCH
import balti.migrate.utilities.CommonToolsKotlin.Companion.ERR_CREATING_MTD
import balti.migrate.utilities.CommonToolsKotlin.Companion.ERR_SCRIPT_MAKING_TRY_CATCH
import balti.migrate.utilities.CommonToolsKotlin.Companion.FILE_PREFIX_BACKUP_SCRIPT
import balti.migrate.utilities.CommonToolsKotlin.Companion.FILE_PREFIX_RETRY_SCRIPT
import balti.migrate.utilities.CommonToolsKotlin.Companion.FILE_SPLIT_APK_NAMES_LIST
import balti.migrate.utilities.CommonToolsKotlin.Companion.MIGRATE_STATUS
import balti.migrate.utilities.CommonToolsKotlin.Companion.PREF_IGNORE_APP_CACHE
import balti.migrate.utilities.CommonToolsKotlin.Companion.PREF_NEW_ICON_METHOD
import balti.migrate.utilities.IconTools
import balti.module.baltitoolbox.functions.FileHandlers.unpackAssetToInternal
import balti.module.baltitoolbox.functions.GetResources.getStringFromRes
import balti.module.baltitoolbox.functions.Misc.getPercentage
import balti.module.baltitoolbox.functions.Misc.tryIt
import balti.module.baltitoolbox.functions.SharedPrefs.getPrefBoolean
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter

class AppBackupEngine(private val busyboxBinaryPath: String) : ParentBackupClass_new(EMPTY) {

    companion object {
        var ICON_STRING = ""
    }

    override val className: String = "AppBackupEngine"

    private var BACKUP_PID = -999

    private val backupUtils by lazy { BackupUtils() }
    private val iconTools by lazy { IconTools() }
    private var suProcess : Process? = null

    private val appList by lazy { appPackets }

    private val apkNamesListFile by lazy { FileX.new(CACHE_DIR, FILE_SPLIT_APK_NAMES_LIST, true) }

    override fun preExecute() {
        super.preExecute()
        // delete all scripts

        globalContext.filesDir.listFiles {
                f -> (f.name.startsWith(FILE_PREFIX_BACKUP_SCRIPT) || f.name.startsWith(FILE_PREFIX_RETRY_SCRIPT)) &&
                f.name.endsWith(".sh")
        }?.forEach { it.delete() }

        globalContext.externalCacheDir?.let {
            it.listFiles {
                    f -> (f.name.startsWith(FILE_PREFIX_BACKUP_SCRIPT) || f.name.startsWith(FILE_PREFIX_RETRY_SCRIPT)) &&
                    f.name.endsWith(".sh")
            }?.forEach { it.delete() }
        }

        CACHE.listFiles { f: FileX ->
            (f.name.startsWith(FILE_PREFIX_BACKUP_SCRIPT) || f.name.startsWith(FILE_PREFIX_RETRY_SCRIPT)) &&
                    f.name.endsWith(".sh")
        }?.forEach { it.delete() }

        apkNamesListFile.createNewFile(true)
    }

    private fun systemAppInstallScript(packageName: String, apkPath: String) {

        val scriptName = "$packageName.sh"
        val scriptLocation = "$pathForAuxFiles/$scriptName"
        val script = FileX.new(scriptLocation, true)

        val pastingDir: String = if (apkPath.contains("priv-app"))
            apkPath.substring(apkPath.indexOf("/priv-app"))
        else apkPath.substring(apkPath.indexOf("/app"))

        val scriptText = "#!sbin/sh\n\n" +
                "\n" +
                "SYSTEM=$(cat /tmp/migrate/SYSTEM)\n" +
                "mkdir -p \$SYSTEM/$pastingDir\n" +
                "mv /tmp/${packageName}.app/*.apk \$SYSTEM/$pastingDir/\n" +
                "rm -rf /tmp/$packageName\n" +
                "rm /tmp/$scriptName\n"


        rootLocation.mkdirs()

        tryIt {
            script.writeOneLine(scriptText)
        }

        // this next line does not work anyway, hence commenting out
        //script.setExecutable(true, false)
    }

    private fun makeBackupScript(): String?{

        try {

            val title = getTitle(R.string.making_app_script)

            resetBroadcast(false, title, PROGRESS_TYPE_MAKING_APP_SCRIPTS)

            appAuxFilesDir.deleteRecursively()

            val ignoreCache = getPrefBoolean(PREF_IGNORE_APP_CACHE, false)

            val scriptFile = FileX.new(CACHE_DIR, "$FILE_PREFIX_BACKUP_SCRIPT.sh", true)
            scriptFile.parentFile?.mkdirs()
            val appAndDataBackupScript = unpackAssetToInternal("backup_app_and_data.sh")

            scriptFile.startWriting(object : FileX.Writer(){
                override fun writeLines() {
                    write("#!sbin/sh\n\n")
                    write("echo \" \"\n")
                    write("sleep 1\n")
                    write("echo \"--- PID: $$\"\n")

                    appList.let {packets ->
                        for (i in 0 until packets.size) {

                            if (BackupServiceKotlin.cancelAll) break

                            val packet = packets[i]

                            val modifiedAppName = "${packet.appName}(${i+1}/${packets.size})"
                            val packageName = packet.PACKAGE_INFO.packageName

                            broadcastProgress(modifiedAppName, modifiedAppName, true, getPercentage(i + 1, packets.size))

                            if (packet.PERMISSION) {
                                backupUtils.makePermissionFile(packageName, pathForAuxFiles, pm)
                            }

                            var versionName: String? = packet.PACKAGE_INFO.versionName
                            versionName = if (versionName == null || versionName == "") "_"
                            else formatName(versionName)

                            var appIconFileName: String = ""
                            if (getPrefBoolean(PREF_NEW_ICON_METHOD, true)) {
                                appIconFileName = backupUtils.makeNewIconFile(packageName, iconTools.getBitmap(packet.PACKAGE_INFO, pm), pathForAuxFiles)
                            }
                            else {
                                val appIcon: String = iconTools.getIconString(packet.PACKAGE_INFO, pm)
                                appIconFileName = backupUtils.makeStringIconFile(packageName, appIcon, pathForAuxFiles)
                            }

                            val echoCopyCommand = "echo \"$MIGRATE_STATUS: $modifiedAppName icon: $appIconFileName\"\n"

                            val scriptCommand = "sh $appAndDataBackupScript " +
                                    "$packageName ${rootLocation.canonicalPath} " +
                                    "${packet.apkPath} ${packet.apkName} " +
                                    "${packet.dataPath} ${packet.dataName} " +
                                    "$busyboxBinaryPath $ignoreCache " +
                                    "${apkNamesListFile.canonicalPath}\n"

                            writeLine(echoCopyCommand)
                            writeLine(scriptCommand)

                            tryIt { if (packet.isSystem) systemAppInstallScript(packageName, packet.apkPath) }

                            backupUtils.makeMetadataFile(pathForAuxFiles, versionName, appIconFileName, packet).let {
                                if (it.isNotBlank()) errors.add("$ERR_CREATING_MTD: ${packet.appName} - $it")
                            }
                        }

                    }

                    write("echo \"--- App files copied ---\"\n")
                }
            })

            scriptFile.file.setExecutable(true)

            return scriptFile.canonicalPath
        }
        catch (e: Exception){
            e.printStackTrace()
            errors.add("$ERR_SCRIPT_MAKING_TRY_CATCH: ${e.message}")
            return null
        }
    }

    private fun runBackupScript(scriptFileLocation: String){

        try {

            if (!FileX.new(scriptFileLocation, true).exists())
                throw Exception(getStringFromRes(R.string.script_file_does_not_exist))

            val title = getTitle(R.string.backingUp)

            resetBroadcast(false, title, PROGRESS_TYPE_APP_PROGRESS)

            suProcess = Runtime.getRuntime().exec(CommonToolsKotlin.SU_INIT)
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
                        errors.add("$ERR_APP_BACKUP_SHELL: $errorLine")
                    else warnings.add("$ERR_APP_BACKUP_SUPPRESSED: $errorLine")

                    return@iterateBufferedReader false
                })

            }

        }
        catch (e: Exception){
            e.printStackTrace()
            errors.add("$ERR_APP_BACKUP_TRY_CATCH: ${e.message}")
        }
    }

    /**
     * The `backup_app_and_data.sh` script creates a file in below format
     * ```
     * <package_name_1>:<apk_name_1>:<size>
     * <package_name_1>:<apk_name_2>:<size>
     * <package_name_1>:<apk_name_3>:<size>
     * <package_name_2>:<apk_name_1>:<size>
     * <package_name_2>:<apk_name_2>:<size>
     * <package_name_2>:<apk_name_3>:<size>
     * <package_name_2>:<apk_name_4>:<size>
     * ```
     * This function parses the file and groups apk names for different package names
     * and then stores them in [appApkFiles].
     */
    private fun populateSplitApkNames(){
        var lastPackageName = ""
        var list = ArrayList<Pair<String, Long>>(0)

        appApkFiles.clear()

        apkNamesListFile.readLines().forEach {

            val parts = it.split(":")
            val currentPackageName = parts[0]
            val apkName = parts[1]
            val size = try { parts[2].toLong() } catch (e: Exception) {-1}

            if (currentPackageName != lastPackageName){

                // first created list will be empty.
                // It will be removed near the end of the function.
                appApkFiles[lastPackageName] = list
                lastPackageName = currentPackageName
                // Create a fresh list.
                // DO NOT USE clear old list and use as it is messing with HashMap.
                list = ArrayList(0)
            }

            list.add(Pair(apkName, size))
        }

        // the last list
        appApkFiles[lastPackageName] = list

        // remove empty list
        appApkFiles.remove("")
    }

    private fun moveAuxFiles(){
        try {
            val title = getTitle(R.string.moving_aux_script)
            resetBroadcast(true, title, PROGRESS_TYPE_MOVING_APP_FILES)
            backupUtils.moveAuxFilesToBackupLocation(pathForAuxFiles, rootLocation.canonicalPath).let {
                it.forEach {
                    errors.add("$ERR_AUX_MOVING_SU: $it")
                }
            }
        }
        catch (e: Exception){
            e.printStackTrace()
            errors.add("$ERR_AUX_MOVING_TRY_CATCH: ${e.message}")
        }
    }

    override suspend fun backgroundProcessing(): Any? {
        val scriptLocation = makeBackupScript()
        moveAuxFiles()
        if (!BackupServiceKotlin.cancelAll) scriptLocation?.let { runBackupScript(it) }
        populateSplitApkNames()
        return null
    }

    override fun postExecute(result: Any?) {
        BACKUP_PID = -999
    }
}