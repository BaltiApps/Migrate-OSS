package balti.migrate.backupEngines.engines

import android.content.pm.PackageInfo
import android.widget.Toast
import balti.migrate.R
import balti.migrate.backupEngines.BackupServiceKotlin
import balti.migrate.backupEngines.ParentBackupClass
import balti.migrate.backupEngines.containers.BackupIntentData
import balti.migrate.backupEngines.utils.BackupUtils
import balti.migrate.extraBackupsActivity.apps.containers.AppBatch
import balti.migrate.utilities.CommonToolKotlin.Companion.ERR_CORRECTION_SHELL
import balti.migrate.utilities.CommonToolKotlin.Companion.ERR_CORRECTION_SUPPRESSED
import balti.migrate.utilities.CommonToolKotlin.Companion.ERR_CORRECTION_TRY_CATCH
import balti.migrate.utilities.CommonToolKotlin.Companion.ERR_TAR_CHECK_TRY_CATCH
import balti.migrate.utilities.CommonToolKotlin.Companion.ERR_TAR_SHELL
import balti.migrate.utilities.CommonToolKotlin.Companion.ERR_TAR_SUPPRESSED
import balti.migrate.utilities.CommonToolKotlin.Companion.ERR_VERIFICATION_TRY_CATCH
import balti.migrate.utilities.CommonToolKotlin.Companion.EXTRA_APP_NAME
import balti.migrate.utilities.CommonToolKotlin.Companion.EXTRA_DEFECT_NUMBER
import balti.migrate.utilities.CommonToolKotlin.Companion.EXTRA_PROGRESS_PERCENTAGE
import balti.migrate.utilities.CommonToolKotlin.Companion.EXTRA_PROGRESS_TYPE
import balti.migrate.utilities.CommonToolKotlin.Companion.EXTRA_PROGRESS_TYPE_CORRECTING
import balti.migrate.utilities.CommonToolKotlin.Companion.EXTRA_PROGRESS_TYPE_VERIFYING
import balti.migrate.utilities.CommonToolKotlin.Companion.EXTRA_RETRY_LOG
import balti.migrate.utilities.CommonToolKotlin.Companion.EXTRA_TAR_CHECK_LOG
import balti.migrate.utilities.CommonToolKotlin.Companion.EXTRA_TITLE
import balti.migrate.utilities.CommonToolKotlin.Companion.FILE_PREFIX_RETRY_SCRIPT
import balti.migrate.utilities.CommonToolKotlin.Companion.FILE_PREFIX_TAR_CHECK
import balti.migrate.utilities.CommonToolKotlin.Companion.MIGRATE_STATUS
import balti.migrate.utilities.CommonToolKotlin.Companion.PREF_NEW_ICON_METHOD
import balti.migrate.utilities.CommonToolKotlin.Companion.PREF_TAR_GZ_INTEGRITY
import balti.migrate.utilities.IconTools
import java.io.*

class VerificationEngine(private val jobcode: Int, private val bd: BackupIntentData,
                         private val appBatch: AppBatch,
                         private val busyboxBinaryPath: String) : ParentBackupClass(bd, "") {

    private var VERIFICATION_PID = -999
    private var TAR_CHECK_CORRECTION_PID = -999

    private val backupUtils by lazy { BackupUtils() }
    private val iconTools by lazy { IconTools() }

    private val pm by lazy { engineContext.packageManager }

    private val allErrors by lazy { ArrayList<String>(0) }
    private val actualErrors by lazy { ArrayList<String>(0) }

    private var suProcess : Process? = null

    init {
        customCancelFunction = {commonTools.tryIt { cancelTask() }}
    }

    private fun addToActualErrors(err: String){
        actualErrors.add(err)
        allErrors.add(err)
    }

    private fun getDataCorrectionCommand(pi: PackageInfo, expectedDataFile: File): String{

        val fullDataPath = pi.applicationInfo.dataDir
        val actualDataName = fullDataPath.substring(fullDataPath.lastIndexOf('/') + 1)
        val dataPathParent = fullDataPath.substring(0, fullDataPath.lastIndexOf('/'))

        return "if [ -e $fullDataPath ]; then\n" +
                "   cd $dataPathParent\n" +
                "   echo \"Copy data: $expectedDataFile\"" +
                "   $busyboxBinaryPath tar -vczpf ${expectedDataFile.absolutePath} $actualDataName\n" +
                "fi\n\n"
    }

    private fun getDataCorrectionCommand(dataName: String): String {
        val packageName = dataName.let { it.substring(0, it.lastIndexOf(".tar.gz")) }
        val expectedDataFile = File(actualDestination, dataName)

        return getDataCorrectionCommand(pm.getPackageInfo(packageName, 0), expectedDataFile)
    }

    private fun verifyBackups(): ArrayList<String>? {

        val allRecovery = ArrayList<String>(0)

        try {

            val title = if (bd.totalParts > 1)
                engineContext.getString(R.string.verifying_backups) + " : " + madePartName
            else engineContext.getString(R.string.verifying_backups)

            actualBroadcast.apply {
                putExtra(EXTRA_PROGRESS_TYPE, EXTRA_PROGRESS_TYPE_VERIFYING)
                putExtra(EXTRA_TITLE, title)
                putExtra(EXTRA_PROGRESS_PERCENTAGE, 0)
            }

            appBatch.appPackets.let { packets ->
                for (i in 0 until packets.size) {

                    if (BackupServiceKotlin.cancelAll) break

                    val packet = packets[i]
                    val pi = packet.PACKAGE_INFO
                    val packageName = pi.packageName

                    actualBroadcast.apply {
                        putExtra(EXTRA_APP_NAME, "verifying: " + pm.getApplicationLabel(pi.applicationInfo))
                        putExtra(EXTRA_PROGRESS_PERCENTAGE, commonTools.getPercentage((i + 1), packets.size))
                    }
                    broadcastProgress()

                    val expectedIconFile = File(actualDestination, "$packageName.icon")
                    val expectedAppDir = File(actualDestination, "$packageName.app")
                    val expectedApkFile = File("$actualDestination/$packageName.app", "$packageName.apk")
                    val expectedDataFile = File("$actualDestination/$packageName.tar.gz")
                    val expectedPermFile = File(actualDestination, "$packageName.perm")

                    if (!expectedIconFile.exists() && sharedPreferences.getBoolean(PREF_NEW_ICON_METHOD, true)) {
                        allRecovery.add("$MIGRATE_STATUS:icon:$packageName")
                    }

                    if (packet.APP &&
                            (!expectedAppDir.exists() || commonTools.getDirLength(expectedAppDir.absolutePath) == 0L) &&
                            (!expectedApkFile.exists() || expectedApkFile.length() == 0L)) {

                        var apkPath = pi.applicationInfo.sourceDir
                        var apkName = apkPath.substring(apkPath.lastIndexOf('/') + 1)
                        apkPath = apkPath.substring(0, apkPath.lastIndexOf('/'))

                        apkName = commonTools.applyNamingCorrectionForShell(apkName)

                        expectedAppDir.absolutePath.let {
                            allRecovery.add(
                                    "echo \"Copy apk(s): $packageName\"\n" +
                                    "rm -rf $it 2> /dev/null" +
                                    "mkdir -p $it\n" +
                                    "cd $apkPath\n" +
                                    "cp *.apk $it/\n\n" +
                                    "mv $it/$apkName $it/${pi.packageName}.apk\n"
                            )
                        }
                    }

                    if (packet.DATA &&
                            (!expectedDataFile.exists() || expectedDataFile.length() == 0L)) {

                        allRecovery.add(
                                getDataCorrectionCommand(pi, expectedDataFile)
                        )

                    }

                    if (packet.PERMISSION && (!expectedPermFile.exists() || expectedPermFile.length() == 0L)) {
                        allRecovery.add("$MIGRATE_STATUS:perm:$packageName")
                    }
                }

                if (sharedPreferences.getBoolean(PREF_TAR_GZ_INTEGRITY, true)){
                    checkTars()?.let {
                        allRecovery.addAll(it)
                    }
                }
            }

            return allRecovery
        }
        catch (e: Exception) {
            e.printStackTrace()
            addToActualErrors("$ERR_VERIFICATION_TRY_CATCH${bd.errorTag}: ${e.message}")
            return null
        }
    }

    private fun checkTars(): ArrayList<String>?{

        try {

            val tarRecovery = ArrayList<String>(0)

            val title = if (bd.totalParts > 1)
                engineContext.getString(R.string.verifying_tar) + " : " + madePartName
            else engineContext.getString(R.string.verifying_tar)

            actualBroadcast.apply {
                putExtra(EXTRA_PROGRESS_TYPE, EXTRA_PROGRESS_TYPE_VERIFYING)
                putExtra(EXTRA_TITLE, title)
                putExtra(EXTRA_PROGRESS_PERCENTAGE, 0)
                removeExtra(EXTRA_APP_NAME)
            }

            broadcastProgress()

            val tarCheckScript = File(engineContext.filesDir.absolutePath, "$FILE_PREFIX_TAR_CHECK${bd.partNumber}.sh")
            val scriptWriter = BufferedWriter(FileWriter(tarCheckScript))

            scriptWriter.write("#!sbin/sh\n\n")
            scriptWriter.write("echo \" \"\n")
            scriptWriter.write("sleep 1\n")
            scriptWriter.write("echo \"--- TAR CHECK PID: $$\"\n")
            scriptWriter.write("cp ${tarCheckScript.absolutePath} ${engineContext.externalCacheDir}/\n")

            scriptWriter.write(
                    "checkData(){\n" +
                            "\n" +
                            "        echo \"--- TAR_GZ \$1 ---\"\n" +
                            "        err=\"\$(${busyboxBinaryPath} gzip -t \"${actualDestination}/\$1\" 2>&1)\"\n" +
                            "        if [[ ! -z \"\$err\" ]]; then\n" +
                            "            echo \"--- ERROR:\$1:\$err ---\"\n" +
                            "        else\n" +
                            "            echo \"--- OK \$1 ---\"\n" +
                            "        fi\n" +
                            "    fi\n" +
                            "\n" +
                            "}\n"
            )

            for (i in 0 until appBatch.appPackets.size) {

                val packet = appBatch.appPackets[i]
                val expectedDataFile = File("$actualDestination/${packet.PACKAGE_INFO.packageName}.tar.gz")

                if (packet.DATA && expectedDataFile.exists() && expectedDataFile.length() == 0L) {
                    scriptWriter.write("checkData ${expectedDataFile.name}\n")
                }
            }

            scriptWriter.write("echo \"--- Tar checks complete ---\"\n")
            scriptWriter.close()

            suProcess = Runtime.getRuntime().exec("su")
            suProcess?.let {
                val suInputStream = BufferedWriter(OutputStreamWriter(it.outputStream))
                val outputStream = BufferedReader(InputStreamReader(it.inputStream))
                val errorStream = BufferedReader(InputStreamReader(it.errorStream))

                suInputStream.write("sh " + tarCheckScript.absolutePath + "\n")
                suInputStream.write("exit\n")
                suInputStream.flush()

                var c = 0

                backupUtils.iterateBufferedReader(outputStream, { line ->

                    if (BackupServiceKotlin.cancelAll) return@iterateBufferedReader true

                    when {
                        line.startsWith("--- TAR CHECK PID:") -> commonTools.tryIt {
                            TAR_CHECK_CORRECTION_PID = line.substring(line.lastIndexOf(" ") + 1).trim().toInt()
                        }
                        line.startsWith("--- TAR_GZ") -> {
                            c++
                            actualBroadcast.apply {
                                putExtra(EXTRA_PROGRESS_PERCENTAGE, commonTools.getPercentage(c, appBatch.appPackets.size))
                            }
                        }
                        line.startsWith("--- ERROR") -> {
                            val dataName = line.let { it1 ->
                                it1.substring(it1.indexOf(' ') + 1, it1.lastIndexOf("---")).trim().split(":")[1]
                            }
                            tarRecovery.add(getDataCorrectionCommand(dataName))
                        }
                    }

                    actualBroadcast.apply {
                        putExtra(EXTRA_TAR_CHECK_LOG, line)
                        putExtra(EXTRA_PROGRESS_PERCENTAGE, commonTools.getPercentage(c, appBatch.appPackets.size))
                    }
                    broadcastProgress()

                    return@iterateBufferedReader line == "--- Tar checks complete ---"

                })

                backupUtils.iterateBufferedReader(errorStream, { errorLine ->

                    var ignorable = false

                    BackupUtils.ignorableWarnings.forEach { warnings ->
                        if (errorLine.endsWith(warnings)) ignorable = true
                    }

                    if (!ignorable)
                        addToActualErrors("$ERR_TAR_SHELL${bd.errorTag}: $errorLine")
                    else allErrors.add("$ERR_TAR_SUPPRESSED${bd.errorTag}: $errorLine")

                    return@iterateBufferedReader false
                }, null)
            }

            return tarRecovery
        }
        catch (e: Exception){
            e.printStackTrace()
            addToActualErrors("$ERR_TAR_CHECK_TRY_CATCH${bd.errorTag}: ${e.message}")
            return null
        }
    }

    private fun correctBackups(defects: ArrayList<String>) {

        if (appBatch.appPackets.size == 0 || defects.size == 0) return

        val title = if (bd.totalParts > 1)
            engineContext.getString(R.string.correcting_errors) + " : " + madePartName
        else engineContext.getString(R.string.correcting_errors)

        actualBroadcast.apply {
            putExtra(EXTRA_TITLE, title)
            putExtra(EXTRA_PROGRESS_TYPE, EXTRA_PROGRESS_TYPE_CORRECTING)
            putExtra(EXTRA_DEFECT_NUMBER, 0)
            putExtra(EXTRA_PROGRESS_PERCENTAGE, 0)
        }

        broadcastProgress()

        try {
            val retryScript = File(engineContext.filesDir.absolutePath, "$FILE_PREFIX_RETRY_SCRIPT${bd.partNumber}.sh")
            val scriptWriter = BufferedWriter(FileWriter(retryScript))

            scriptWriter.write("#!sbin/sh\n\n")
            scriptWriter.write("echo \" \"\n")
            scriptWriter.write("sleep 1\n")
            scriptWriter.write("echo \"--- RECOVERY PID: $$\"\n")
            scriptWriter.write("cp ${retryScript.absolutePath} ${engineContext.externalCacheDir}/\n")

            for (i in 0 until defects.size){
                if (BackupServiceKotlin.cancelAll) break

                val defect = defects[i]
                if (defect.startsWith(MIGRATE_STATUS)){
                    val parts = defect.split(":")
                    if (parts.size == 3){
                        when (parts[1]){
                            "icon" -> {
                                val pi = pm.getPackageInfo(parts[2], 0)
                                backupUtils.makeIconFile(parts[2], iconTools.getIconString(pi, pm), actualDestination)
                            }
                            "perm" -> {
                                backupUtils.makePermissionFile(parts[2], actualDestination, pm)
                            }
                        }
                        scriptWriter.write("echo \"--- DEFECT: ${i + 1}\"\n")
                        scriptWriter.write("echo \"$defect\"")
                    }
                }
                else {
                    scriptWriter.write("echo \"--- DEFECT: ${i + 1}\"\n")
                    scriptWriter.write(defect)
                }
            }

            scriptWriter.write("echo \"--- Retry complete ---\"\n")
            scriptWriter.close()

            suProcess = Runtime.getRuntime().exec("su")
            suProcess?.let {
                val suInputStream = BufferedWriter(OutputStreamWriter(it.outputStream))
                val outputStream = BufferedReader(InputStreamReader(it.inputStream))
                val errorStream = BufferedReader(InputStreamReader(it.errorStream))

                suInputStream.write("sh " + retryScript.absolutePath + "\n")
                suInputStream.write("exit\n")
                suInputStream.flush()

                backupUtils.iterateBufferedReader(outputStream, {line ->

                    if (BackupServiceKotlin.cancelAll) return@iterateBufferedReader true

                    if (line.startsWith("--- RECOVERY PID:")){
                        commonTools.tryIt {
                            VERIFICATION_PID = line.substring(line.lastIndexOf(" ") + 1).trim().toInt()
                        }
                    }
                    else if (line.startsWith("--- DEFECT:")){
                        val defectNumber = line.substring(line.lastIndexOf(" ") + 1).trim().toInt()
                        actualBroadcast.apply {
                            putExtra(EXTRA_DEFECT_NUMBER, defectNumber)
                            putExtra(EXTRA_PROGRESS_PERCENTAGE, commonTools.getPercentage(defectNumber, defects.size))
                        }
                    }

                    actualBroadcast.putExtra(EXTRA_RETRY_LOG, line)
                    broadcastProgress()

                    return@iterateBufferedReader line == "--- Retry complete ---"
                })

                commonTools.tryIt { it.waitFor() }

                backupUtils.iterateBufferedReader(errorStream, {errorLine ->

                    var ignorable = false

                    BackupUtils.ignorableWarnings.forEach {warnings ->
                        if (errorLine.endsWith(warnings)) ignorable = true
                    }

                    if (!ignorable)
                        addToActualErrors("$ERR_CORRECTION_SHELL${bd.errorTag}: $errorLine")
                    else allErrors.add("$ERR_CORRECTION_SUPPRESSED${bd.errorTag}: $errorLine")

                    return@iterateBufferedReader false
                }, null)
            }
        }
        catch (e: Exception){
            e.printStackTrace()
            addToActualErrors("$ERR_CORRECTION_TRY_CATCH${bd.errorTag}: ${e.message}")
        }
    }

    private fun cancelTask() {
        if (VERIFICATION_PID != -999) {
            commonTools.tryIt {
                val killProcess = Runtime.getRuntime().exec("su")

                val writer = BufferedWriter(OutputStreamWriter(killProcess.outputStream))
                fun killId(pid: Int) {
                    writer.write("kill -9 $pid\n")
                    writer.write("kill -15 $pid\n")
                }

                killId(VERIFICATION_PID)
                killId(TAR_CHECK_CORRECTION_PID)

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
        val defects = verifyBackups()
        if (defects != null && defects.size != 0)
            correctBackups(defects)

        return 0
    }

    override fun onPostExecute(result: Any?) {
        super.onPostExecute(result)
        VERIFICATION_PID = -999
        onBackupComplete.onBackupComplete(jobcode, actualErrors.size == 0, allErrors)
    }
}