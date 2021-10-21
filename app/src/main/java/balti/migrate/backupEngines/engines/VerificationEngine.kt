package balti.migrate.backupEngines.engines

import android.content.pm.PackageInfo
import balti.filex.FileX
import balti.filex.Quad
import balti.migrate.AppInstance.Companion.CACHE_DIR
import balti.migrate.AppInstance.Companion.appApkFiles
import balti.migrate.R
import balti.migrate.backupEngines.BackupServiceKotlin
import balti.migrate.backupEngines.ParentBackupClass
import balti.migrate.backupEngines.containers.BackupIntentData
import balti.migrate.backupEngines.utils.BackupUtils
import balti.migrate.extraBackupsActivity.apps.containers.AppPacket
import balti.migrate.utilities.CommonToolsKotlin
import balti.migrate.utilities.CommonToolsKotlin.Companion.DIR_APK_FILES_SIZES
import balti.migrate.utilities.CommonToolsKotlin.Companion.ERR_APK_CHECK_TRY_CATCH
import balti.migrate.utilities.CommonToolsKotlin.Companion.ERR_CORRECTION_AUX_MOVING_SU
import balti.migrate.utilities.CommonToolsKotlin.Companion.ERR_CORRECTION_AUX_MOVING_TRY_CATCH
import balti.migrate.utilities.CommonToolsKotlin.Companion.ERR_CORRECTION_SHELL
import balti.migrate.utilities.CommonToolsKotlin.Companion.ERR_CORRECTION_SUPPRESSED
import balti.migrate.utilities.CommonToolsKotlin.Companion.ERR_CORRECTION_TRY_CATCH
import balti.migrate.utilities.CommonToolsKotlin.Companion.ERR_TAR_CHECK_TRY_CATCH
import balti.migrate.utilities.CommonToolsKotlin.Companion.ERR_TAR_SHELL
import balti.migrate.utilities.CommonToolsKotlin.Companion.ERR_TAR_SUPPRESSED
import balti.migrate.utilities.CommonToolsKotlin.Companion.ERR_VERIFICATION_TRY_CATCH
import balti.migrate.utilities.CommonToolsKotlin.Companion.EXTRA_PROGRESS_TYPE_CORRECTING
import balti.migrate.utilities.CommonToolsKotlin.Companion.EXTRA_PROGRESS_TYPE_VERIFYING
import balti.migrate.utilities.CommonToolsKotlin.Companion.FILE_PREFIX_RETRY_SCRIPT
import balti.migrate.utilities.CommonToolsKotlin.Companion.FILE_PREFIX_TAR_CHECK
import balti.migrate.utilities.CommonToolsKotlin.Companion.LOG_CORRECTION_NEEDED
import balti.migrate.utilities.CommonToolsKotlin.Companion.MIGRATE_STATUS
import balti.migrate.utilities.CommonToolsKotlin.Companion.PREF_NEW_ICON_METHOD
import balti.migrate.utilities.CommonToolsKotlin.Companion.PREF_TAR_GZ_INTEGRITY
import balti.migrate.utilities.CommonToolsKotlin.Companion.SU_INIT
import balti.migrate.utilities.CommonToolsKotlin.Companion.WARNING_APK_SIZE_INFO_WRONG
import balti.migrate.utilities.CommonToolsKotlin.Companion.WARNING_CASTING_APK_SIZE
import balti.migrate.utilities.IconTools
import balti.module.baltitoolbox.functions.Misc.getPercentage
import balti.module.baltitoolbox.functions.Misc.tryIt
import balti.module.baltitoolbox.functions.SharedPrefs.getPrefBoolean
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter

class VerificationEngine(private val jobcode: Int, private val bd: BackupIntentData,
                         private val appList: ArrayList<AppPacket>,
                         private val busyboxBinaryPath: String) : ParentBackupClass(bd, "") {

    private var CORRECTION_PID = -999
    private var TAR_CHECK_CORRECTION_PID = -999

    private val backupUtils by lazy { BackupUtils() }
    private val iconTools by lazy { IconTools() }

    private val pm by lazy { engineContext.packageManager }

    private val allErrors by lazy { ArrayList<String>(0) }
    private val actualErrors by lazy { ArrayList<String>(0) }
    private val warnings by lazy { ArrayList<String>(0) }

    private var suProcess : Process? = null

    private var lastProgress = 0

    private var allFiles: List<Quad<String, Boolean, Long, Long>>? = null

    private val dataFileNames = ArrayList<String>(0)
    private val dataFileSizes = ArrayList<Long>(0)

    private val packagesWithApkCorrection by lazy { ArrayList<String>(0) }

    /**
     * A directory where txt files are stored listing base and split APK sizes, after correction.
     * Name of a file in this directory = package name of the app.
     */
    private val apkFilesSizesDirPath by lazy { CACHE.canonicalPath + "/" + DIR_APK_FILES_SIZES }

    private fun addToActualErrors(err: String){
        actualErrors.add(err)
        allErrors.add(err)
    }

    private fun getDataCorrectionCommand(pi: PackageInfo, expectedDataFile: FileX): String{

        val fullDataPath = pi.applicationInfo.dataDir
        val actualDataName = fullDataPath.substring(fullDataPath.lastIndexOf('/') + 1)
        val dataPathParent = fullDataPath.substring(0, fullDataPath.lastIndexOf('/'))

        return "if [ -e $fullDataPath ]; then\n" +
                "   cd $dataPathParent\n" +
                "   echo \"Copy data: ${expectedDataFile.name}\"\n" +
                "   chmod +x $busyboxBinaryPath\n" +
                "   $busyboxBinaryPath tar -vczpf ${expectedDataFile.canonicalPath} $actualDataName\n" +
                "fi\n\n"
    }

    private fun getDataCorrectionCommand(dataName: String): String {
        val packageName = dataName.let { it.substring(0, it.lastIndexOf(".tar.gz")) }
        val expectedDataFile = FileX.new(actualDestination, dataName)

        return getDataCorrectionCommand(pm.getPackageInfo(packageName, 0), expectedDataFile)
    }

    private fun verifyBackups(): ArrayList<String>? {

        if (BackupServiceKotlin.cancelAll) return null

        val allRecovery = ArrayList<String>(0)

        try {

            val title = getTitle(R.string.verifying_backups)

            resetBroadcast(true, title, EXTRA_PROGRESS_TYPE_VERIFYING)

            appAuxFilesDir.run { deleteRecursively(); mkdirs() }

            val permFileNames = ArrayList<String>(0)
            val permFileSizes = ArrayList<Long>(0)

            val iconFiles = ArrayList<String>(0)

            rootLocation.exists()
            allFiles = rootLocation.listEverythingInQuad()

            allFiles?.forEach {
                val name = it.first
                val isDir = it.second
                val size = it.third
                if (name.endsWith(".tar.gz") && !isDir){
                    dataFileNames.add(name)
                    dataFileSizes.add(size)
                }
                if (name.endsWith(".perm") && !isDir){
                    permFileNames.add(name)
                    permFileSizes.add(size)
                }
                if ((name.endsWith(".icon") || name.endsWith(".png")) && !isDir){
                    iconFiles.add(name)
                }
            }

            val size = appList.size
            for (i in appList.indices) {

                if (BackupServiceKotlin.cancelAll) break

                val packet = appList[i]
                val pi = packet.PACKAGE_INFO
                val packageName = pi.packageName
                val appName = pm.getApplicationLabel(pi.applicationInfo).toString()
                val displayTitle = "$appName(${i+1}/${size})"

                // do not show progress if traditional filex, as it will be very fast
                // find progress if filex is non-traditional
                //val progress = if (FileXInit.isTraditional) -1 else Misc.getPercentage((i+1), size)
                //broadcastProgress(displayTitle, "verifying: $displayTitle", showNotification, progress)

                /*val expectedAppDir = FileX.new(actualDestination, "$packageName.app")
                val expectedApkFile = FileX.new("$actualDestination/$packageName.app", "$packageName.apk")
                val expectedDataFile = FileX.new("$actualDestination/$packageName.tar.gz")
                val expectedPermFile = FileX.new(actualDestination, "$packageName.perm")*/

                if (getPrefBoolean(PREF_NEW_ICON_METHOD, true) ) {
                    if (!iconFiles.contains("$packageName.png")) allRecovery.add("$MIGRATE_STATUS:icon_new:$packageName")
                }
                else {
                    if (!iconFiles.contains("$packageName.icon")) allRecovery.add("$MIGRATE_STATUS:icon_old:$packageName")
                }

                /*if (packet.APP) {

                    val existsAppDir = allAppDirs.contains("$packageName.app")
                    val baseApkFileSize: Long =
                            FileX.new("$actualDestination/$packageName.app", "$packageName.apk").run {
                                if (exists()) length()
                                else 0
                            }
                            //100L

                    if (!existsAppDir || baseApkFileSize == 0L) {

                        var apkPath = pi.applicationInfo.sourceDir
                        var apkName = apkPath.substring(apkPath.lastIndexOf('/') + 1)
                        apkPath = apkPath.substring(0, apkPath.lastIndexOf('/'))

                        apkName = commonTools.applyNamingCorrectionForShell(apkName)

                        val expectedAppDir = FileX.new(actualDestination, "$packageName.app")

                        expectedAppDir.canonicalPath.let {
                            if (CommonToolsKotlin.isDeletable(expectedAppDir)) {
                                allRecovery.add(
                                        "echo \"Copy apk(s): $packageName\"\n" +
                                                "rm -rf $it 2> /dev/null\n" +
                                                "mkdir -p $it\n" +
                                                "cd $apkPath\n" +
                                                "cp *.apk $it/\n\n" +
                                                "mv $it/$apkName $it/${pi.packageName}.apk\n"
                                )
                            }
                        }
                        //broadcastProgress(displayTitle, "${LOG_CORRECTION_NEEDED}: $packageName : \"app\" App directory - $existsAppDir, Base apk size - $baseApkFileSize", showNotification)
                    }
                }*/

                if (packet.DATA) {

                    val dataName = "$packageName.tar.gz"
                    val index = dataFileNames.indexOf(dataName)
                    val existsData = index != -1
                    val sizeData = if (existsData) dataFileSizes[index] else 0L

                    if (!existsData || sizeData == 0L) {
                        allRecovery.add(getDataCorrectionCommand(pi, FileX.new(actualDestination, dataName)))
                        broadcastProgress(displayTitle, "${LOG_CORRECTION_NEEDED}: $packageName : \"data\" Exists - $existsData, Size - $sizeData", false)
                    }
                }

                if (packet.PERMISSION) {

                    val permName = "$packageName.perm"
                    val index = permFileNames.indexOf(permName)
                    val existsPerm = index != -1
                    val sizePerm = if (existsPerm) permFileSizes[index] else 0L

                    if (!existsPerm || sizePerm == 0L) {
                        allRecovery.add("$MIGRATE_STATUS:perm:$packageName")
                        broadcastProgress(displayTitle, "${LOG_CORRECTION_NEEDED}: $packageName : \"perm\" Exists - $existsPerm, Size - $sizePerm", false)
                    }
                }
            }

            if (getPrefBoolean(PREF_TAR_GZ_INTEGRITY, true)) {
                checkTars()?.let {
                    allRecovery.addAll(it)
                }
            }

            return allRecovery
        }
        catch (e: Exception) {
            e.printStackTrace()
            addToActualErrors("$ERR_VERIFICATION_TRY_CATCH: ${e.message}")
            return null
        }
    }

    private fun checkApks(): ArrayList<String>?{

        try {

            val title = getTitle(R.string.verifying_apks)
            resetBroadcast(true, title, EXTRA_PROGRESS_TYPE_VERIFYING)

            val corrections = ArrayList<String>(0)
            packagesWithApkCorrection.clear()

            fun getCorrection(appPacket: AppPacket): String {
                return appPacket.run {
                    val expectedAppDir = rootLocation.canonicalPath + "/${packageName}.app"
                    broadcastProgress(
                        "",
                        "${LOG_CORRECTION_NEEDED}: ${appPacket.packageName} : \"app\" App directory - $expectedAppDir",
                        false
                    )
                    expectedAppDir.let {
                        "echo \"Copy apk(s): $packageName\"\n" +
                                "rm -rf $it/*.apk 2> /dev/null\n" +
                                "cd $apkPath\n" +
                                "cp *.apk $it/\n\n" +
                                "mv $it/$apkName $it/${packageName}.apk\n\n" +
                                "cd $it\n" +
                                "ls -al * | awk '{printf \"%s %s\\n\", \$5, \$8}' > ${apkFilesSizesDirPath}/${packageName}\n\n"
                    }
                }
            }

            FileX.new(apkFilesSizesDirPath, true).let {
                if (it.exists()) it.deleteRecursively()
                it.mkdirs()
            }

            appList.forEach { appPacket ->
                if (appPacket.APP) {

                    var correctionNeeded = false
                    val infoList = appApkFiles[appPacket.packageName] ?: listOf()

                    val base = infoList.find { it.first == "${appPacket.packageName}.apk" }
                    if (base == null || base.second <= 0) correctionNeeded = true
                    else {
                        for (pair in infoList) {
                            if (pair.second <= 0) {
                                correctionNeeded = true
                                break
                            }
                        }
                    }

                    packagesWithApkCorrection.add(appPacket.packageName)
                    if (correctionNeeded) corrections.add(getCorrection(appPacket))
                }
            }

            return corrections
        }
        catch (e: Exception){
            e.printStackTrace()
            addToActualErrors("$ERR_APK_CHECK_TRY_CATCH: ${e.message}")
            return null
        }
    }

    private fun checkTars(): ArrayList<String>?{

        try {

            if (BackupServiceKotlin.cancelAll) return null

            val tarRecovery = ArrayList<String>(0)
            lastProgress = 0

            val title = getTitle(R.string.verifying_tar)

            resetBroadcast(false, title, EXTRA_PROGRESS_TYPE_VERIFYING)

            val tarCheckScript = FileX.new(engineContext.filesDir.canonicalPath, "$FILE_PREFIX_TAR_CHECK.sh", true)
            //val scriptWriter = BufferedWriter(FileWriter(tarCheckScript))

            tarCheckScript.startWriting(object : FileX.Writer(){
                override fun writeLines() {

                    write("#!sbin/sh\n\n")
                    write("echo \" \"\n")
                    write("sleep 1\n")
                    write("echo \"--- TAR CHECK PID: $$\"\n")
                    write("cp ${tarCheckScript.absolutePath} ${CACHE_DIR}/${tarCheckScript.name}\n")
                    write("chown ${myUid}:${myUid} ${CACHE_DIR}/${tarCheckScript.name}\n")

                    writeLine(
                            "checkData(){\n" +
                                    "\n" +
                                    "    echo \"--- TAR_GZ \$1 ---\"\n" +
                                    "    err=\"\$(${busyboxBinaryPath} gzip -t \"${rootLocation.canonicalPath}/\$1\" 2>&1)\"\n" +
                                    "    if [[ ! -z \"\$err\" ]]; then\n" +
                                    "        echo \"--- ERROR:\$1:\$err ---\"\n" +
                                    "    else\n" +
                                    "        echo \"--- OK:\$1 ---\"\n" +
                                    "    fi\n" +
                                    "\n" +
                                    "}\n"
                    )

                    writeLine("")
                    writeLine("chmod +x $busyboxBinaryPath")
                    writeLine("")

                    for (i in appList.indices) {

                        val packet = appList[i]

                        val expectedName = "${packet.packageName}.tar.gz"

                        if (packet.DATA && dataFileNames.contains(expectedName)) {
                            write("checkData ${expectedName}\n")
                        }
                    }

                    writeLine("echo \"--- Tar checks complete ---\"\n")
                }
            })

            suProcess = Runtime.getRuntime().exec(CommonToolsKotlin.SU_INIT)
            suProcess?.let {
                val suInputStream = BufferedWriter(OutputStreamWriter(it.outputStream))
                val outputStream = BufferedReader(InputStreamReader(it.inputStream))
                val errorStream = BufferedReader(InputStreamReader(it.errorStream))

                suInputStream.write("sh " + tarCheckScript.absolutePath + "\n")
                suInputStream.write("exit\n")
                suInputStream.flush()

                var c = 0
                var progress = 0
                var log = ""

                backupUtils.iterateBufferedReader(outputStream, { line ->

                    if (BackupServiceKotlin.cancelAll) {
                        cancelTask(suProcess, TAR_CHECK_CORRECTION_PID)
                        return@iterateBufferedReader true
                    }

                    when {
                        line.startsWith("--- TAR CHECK PID:") -> tryIt {
                            log = line
                            TAR_CHECK_CORRECTION_PID = line.substring(line.lastIndexOf(" ") + 1).trim().toInt()
                        }
                        line.startsWith("--- TAR_GZ") -> {
                            c++
                            progress = getPercentage(c, appList.size)
                            log = ""
                        }
                        line.startsWith("--- ERROR") -> {
                            val dataName = line.let { it1 ->
                                it1.trim().split(":")[1]
                            }
                            tarRecovery.add(getDataCorrectionCommand(dataName))
                            log = "Error: $dataName\n$line"
                        }
                        else -> log = line
                    }

                    broadcastProgress("", log, progress != lastProgress, progress)
                    lastProgress = progress

                    return@iterateBufferedReader line == "--- Tar checks complete ---"

                })

                backupUtils.iterateBufferedReader(errorStream, { errorLine ->

                    var ignorable = false

                    BackupUtils.ignorableWarnings.forEach { warnings ->
                        if (errorLine.endsWith(warnings)) ignorable = true
                    }

                    if (!ignorable)
                        addToActualErrors("$ERR_TAR_SHELL: $errorLine")
                    else allErrors.add("$ERR_TAR_SUPPRESSED: $errorLine")

                    return@iterateBufferedReader false
                }, null)
            }

            return tarRecovery
        }
        catch (e: Exception){
            e.printStackTrace()
            addToActualErrors("$ERR_TAR_CHECK_TRY_CATCH: ${e.message}")
            return null
        }
    }

    private fun correctBackups(defects: ArrayList<String>) {

        if (appList.size == 0 || defects.size == 0) return

        lastProgress = 0

        val title = getTitle(R.string.correcting_errors)

        resetBroadcast(false, title, EXTRA_PROGRESS_TYPE_CORRECTING)

        try {
            val retryScript = FileX.new(engineContext.filesDir.canonicalPath, "$FILE_PREFIX_RETRY_SCRIPT.sh", true)
            retryScript.startWriting(object : FileX.Writer(){
                override fun writeLines() {
                    write("#!sbin/sh\n\n")
                    write("echo \" \"\n")
                    write("sleep 1\n")
                    write("echo \"--- RECOVERY PID: $$\"\n")
                    write("cp ${retryScript.absolutePath} ${CACHE_DIR}/${retryScript.name}\n")
                    write("chown ${myUid}:${myUid} ${CACHE_DIR}/${retryScript.name}\n\n")

                    for (i in 0 until defects.size){
                        if (BackupServiceKotlin.cancelAll) break

                        val defect = defects[i]
                        if (defect.startsWith(MIGRATE_STATUS)){
                            val parts = defect.split(":")
                            if (parts.size == 3){
                                when (parts[1]){
                                    "icon_new" -> {
                                        val pi = pm.getPackageInfo(parts[2], 0)
                                        backupUtils.makeNewIconFile(parts[2], iconTools.getBitmap(pi, pm), pathForAuxFiles)
                                    }
                                    "icon_old" -> {
                                        val pi = pm.getPackageInfo(parts[2], 0)
                                        backupUtils.makeStringIconFile(parts[2], iconTools.getIconString(pi, pm), pathForAuxFiles)
                                    }
                                    "perm" -> {
                                        backupUtils.makePermissionFile(
                                                parts[2], pathForAuxFiles, pm)
                                    }
                                }
                                write("echo \"--- DEFECT: ${i + 1}\"\n")
                                write("echo \"$defect\"\n")
                            }
                        }
                        else {
                            write("echo \"--- DEFECT: ${i + 1}\"\n")
                            writeLine(defect)
                        }
                    }

                    write("echo \"--- Retry complete ---\"\n")
                }
            })



            suProcess = Runtime.getRuntime().exec(SU_INIT)
            suProcess?.let {
                val suInputStream = BufferedWriter(OutputStreamWriter(it.outputStream))
                val outputStream = BufferedReader(InputStreamReader(it.inputStream))
                val errorStream = BufferedReader(InputStreamReader(it.errorStream))

                suInputStream.write("sh " + retryScript.absolutePath + "\n")
                suInputStream.write("exit\n")
                suInputStream.flush()

                var progress = 0
                var defectNumber = 0

                backupUtils.iterateBufferedReader(outputStream, {line ->

                    if (BackupServiceKotlin.cancelAll) {
                        cancelTask(suProcess, CORRECTION_PID)
                        return@iterateBufferedReader true
                    }

                    if (line.startsWith("--- RECOVERY PID:")){
                        tryIt {
                            CORRECTION_PID = line.substring(line.lastIndexOf(" ") + 1).trim().toInt()
                        }
                    }
                    else if (line.startsWith("--- DEFECT:")){
                        defectNumber = line.substring(line.lastIndexOf(" ") + 1).trim().toInt()
                        progress = getPercentage(defectNumber, defects.size)
                    }


                    broadcastProgress("${engineContext.getString(R.string.defect_no)} $defectNumber",
                            line, progress != lastProgress, progress)

                    lastProgress = progress

                    return@iterateBufferedReader line == "--- Retry complete ---"
                })

                tryIt { it.waitFor() }

                backupUtils.iterateBufferedReader(errorStream, {errorLine ->

                    var ignorable = false

                    BackupUtils.ignorableWarnings.forEach {warnings ->
                        if (errorLine.endsWith(warnings)) ignorable = true
                    }

                    if (!ignorable)
                        addToActualErrors("$ERR_CORRECTION_SHELL: $errorLine")
                    else allErrors.add("$ERR_CORRECTION_SUPPRESSED: $errorLine")

                    return@iterateBufferedReader false
                }, null)
            }
        }
        catch (e: Exception){
            e.printStackTrace()
            addToActualErrors("$ERR_CORRECTION_TRY_CATCH: ${e.message}")
        }
    }

    private fun readCorrectedApkSizes(){

        FileX.new(apkFilesSizesDirPath, true).listFiles()?.forEach { file ->
            val packageName = file.name
            val list = ArrayList<Pair<String, Long>>(0)
            file.readLines().forEach {
                if (it.isNotBlank()) {

                    val split = it.split(' ')
                    if (split.size == 2) {
                        val size: Long =
                            try {
                                it.split(' ')[0].trim().toLong()
                            } catch (e: Exception) {
                                warnings.add("${WARNING_CASTING_APK_SIZE}: Package: $packageName. Line: $it")
                                0L
                            }
                        val apkName = it.split(' ')[1].trim()

                        list.add(Pair(apkName, size))

                    } else warnings.add("${WARNING_APK_SIZE_INFO_WRONG}: Package: $packageName. Line: $it. Split size: ${split.size}")
                }
            }

            appApkFiles[packageName] = list
        }

    }

    private fun moveAuxFiles(){
        try {
            val title = getTitle(R.string.moving_aux_script)
            resetBroadcast(true, title, EXTRA_PROGRESS_TYPE_CORRECTING)
            backupUtils.moveAuxFilesToBackupLocation(pathForAuxFiles, rootLocation.canonicalPath).let {
                it.forEach {
                    addToActualErrors("$ERR_CORRECTION_AUX_MOVING_SU: $it")
                }
            }
        }
        catch (e: Exception){
            e.printStackTrace()
            addToActualErrors("$ERR_CORRECTION_AUX_MOVING_TRY_CATCH: ${e.message}")
        }
    }

    override suspend fun doInBackground(arg: Any?): Any {

        val defects = ArrayList<String>(0)
        checkApks()?.let { defects.addAll(it) }
        verifyBackups()?.let { defects.addAll(it) }
        if (!BackupServiceKotlin.cancelAll && defects.size != 0)
            correctBackups(defects)
        if (packagesWithApkCorrection.isNotEmpty()) readCorrectedApkSizes()
        if (appAuxFilesDir.exists() && !appAuxFilesDir.isEmpty && defects.any { it.startsWith(MIGRATE_STATUS) })
            moveAuxFiles()

        return 0
    }

    override fun postExecuteFunction() {
        CORRECTION_PID = -999
        TAR_CHECK_CORRECTION_PID = -999
        onEngineTaskComplete.onComplete(jobcode, actualErrors, jobResults = allErrors, jobWarnings = warnings)
    }
}