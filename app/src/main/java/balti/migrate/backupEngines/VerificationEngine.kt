package balti.migrate.backupEngines

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.AsyncTask
import android.widget.Toast
import balti.migrate.R
import balti.migrate.backupEngines.utils.BackupDependencyComponent
import balti.migrate.backupEngines.utils.BackupUtils
import balti.migrate.backupEngines.utils.DaggerBackupDependencyComponent
import balti.migrate.backupEngines.utils.OnBackupComplete
import balti.migrate.extraBackupsActivity.apps.AppBatch
import balti.migrate.utilities.CommonToolKotlin
import balti.migrate.utilities.CommonToolKotlin.Companion.ACTION_BACKUP_PROGRESS
import balti.migrate.utilities.CommonToolKotlin.Companion.DEFECT_NUMBER
import balti.migrate.utilities.CommonToolKotlin.Companion.EXTRA_APP_NAME
import balti.migrate.utilities.CommonToolKotlin.Companion.EXTRA_BACKUP_NAME
import balti.migrate.utilities.CommonToolKotlin.Companion.EXTRA_PART_NUMBER
import balti.migrate.utilities.CommonToolKotlin.Companion.EXTRA_PROGRESS_PERCENTAGE
import balti.migrate.utilities.CommonToolKotlin.Companion.EXTRA_PROGRESS_TYPE
import balti.migrate.utilities.CommonToolKotlin.Companion.EXTRA_PROGRESS_TYPE_CORRECTING
import balti.migrate.utilities.CommonToolKotlin.Companion.EXTRA_PROGRESS_TYPE_VERIFYING
import balti.migrate.utilities.CommonToolKotlin.Companion.EXTRA_RETRY_LOG
import balti.migrate.utilities.CommonToolKotlin.Companion.EXTRA_TITLE
import balti.migrate.utilities.CommonToolKotlin.Companion.EXTRA_TOTAL_PARTS
import balti.migrate.utilities.CommonToolKotlin.Companion.FILE_PREFIX_RETRY_SCRIPT
import balti.migrate.utilities.CommonToolKotlin.Companion.MIGRATE_STATUS
import balti.migrate.utilities.CommonToolKotlin.Companion.PREF_NEW_ICON_METHOD
import java.io.*
import javax.inject.Inject

class VerificationEngine(private val jobcode: Int, private val bd: BackupIntentData,
                         private val appBatch: AppBatch,
                         private val busyboxBinaryPath: String) :AsyncTask<Any, Any, Any>() {

    @Inject lateinit var engineContext: Context
    @Inject lateinit var sharedPrefs: SharedPreferences

    private var VERIFICATION_PID = -999
    private var isBackupCancelled = false

    private val onBackupComplete by lazy { engineContext as OnBackupComplete }

    private val backupDependencyComponent: BackupDependencyComponent
            by lazy { DaggerBackupDependencyComponent.create() }

    private val commonTools by lazy { CommonToolKotlin(engineContext) }
    private val backupUtils by lazy { BackupUtils() }

    private val actualBroadcast by lazy {
        Intent(ACTION_BACKUP_PROGRESS).apply {
            putExtra(EXTRA_BACKUP_NAME, bd.backupName)
            putExtra(EXTRA_PROGRESS_TYPE, "")
            putExtra(EXTRA_TOTAL_PARTS, bd.totalParts)
            putExtra(EXTRA_PART_NUMBER, bd.partNumber)
            putExtra(EXTRA_PROGRESS_PERCENTAGE, 0)
        }
    }
    private val pm by lazy { engineContext.packageManager }
    private val allErrors by lazy { ArrayList<String>(0) }
    private val madePartName by lazy { commonTools.getMadePartName(bd) }
    private val actualDestination by lazy { "${bd.destination}/${bd.backupName}" }

    private var suProcess : Process? = null

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

    private fun verifyBackups(): ArrayList<String>? {

        val allRecovery = ArrayList<String>(0)

        try {

            val title = if (bd.totalParts > 1)
                engineContext.getString(R.string.verifying_backups) + " : " + madePartName
            else engineContext.getString(R.string.verifying_backups)

            actualBroadcast.putExtra(EXTRA_PROGRESS_TYPE, EXTRA_PROGRESS_TYPE_VERIFYING)

            appBatch.appPackets.let { packets ->
                for (i in 0 until packets.size) {

                    if (isBackupCancelled) break

                    val packet = packets[i]
                    val pi = packet.PACKAGE_INFO
                    val packageName = pi.packageName

                    actualBroadcast.apply {
                        putExtra(EXTRA_TITLE, title)
                        putExtra(EXTRA_APP_NAME, "verifying: " + pm.getApplicationLabel(pi.applicationInfo))
                                .putExtra(EXTRA_PROGRESS_PERCENTAGE, commonTools.getPercentage((i + 1), packets.size))
                    }
                    commonTools.LBM?.sendBroadcast(actualBroadcast)

                    val expectedIconFile = File(actualDestination, "$packageName.icon")
                    val expectedAppDir = File(actualDestination, "$packageName.app")
                    val expectedApkFile = File("$actualDestination/$packageName.app", "$packageName.apk")
                    val expectedDataFile = File("$actualDestination/$packageName.tar.gz")
                    val expectedPermFile = File(actualDestination, "$packageName.perm")

                    if (!expectedIconFile.exists() && sharedPrefs.getBoolean(PREF_NEW_ICON_METHOD, true)) {
                        allRecovery.add("$MIGRATE_STATUS:icon:$packageName")
                    }

                    if (packet.APP &&
                            (!expectedAppDir.exists() || commonTools.getDirLength(expectedAppDir.absolutePath) == 0L) &&
                            (!expectedApkFile.exists() || expectedApkFile.length() == 0L)) {

                        var apkPath = pi.applicationInfo.sourceDir
                        var apkName = apkPath.substring(apkPath.lastIndexOf('/') + 1)
                        apkPath = apkPath.substring(0, apkPath.lastIndexOf('/'))

                        apkName = commonTools.applyNamingCorrectionForShell(apkName)

                        allRecovery.add("echo \"Copy apk(s): $packageName\"\n" +
                                "rm -rf $expectedAppDir 2> /dev/null" +
                                "mkdir -p $expectedAppDir\n" +
                                "cd $apkPath\n" +
                                "cp *.apk $expectedAppDir/\n\n" +
                                "mv $expectedAppDir/$apkName $expectedAppDir/${pi.packageName}.apk\n"
                        )
                    }

                    if (packet.DATA &&
                            (!expectedDataFile.exists() || expectedDataFile.length() == 0L)) {

                        val fullDataPath = pi.applicationInfo.dataDir
                        val actualDataName = fullDataPath.substring(fullDataPath.lastIndexOf('/') + 1)
                        val dataPathParent = fullDataPath.substring(0, fullDataPath.lastIndexOf('/'))

                        allRecovery.add(
                                "if [ -e $fullDataPath ]; then\n" +
                                        "   cd $dataPathParent\n" +
                                        "   echo \"Copy data: $expectedDataFile\"" +
                                        "   $busyboxBinaryPath tar -vczpf ${expectedDataFile.absolutePath} $actualDataName\n" +
                                        "fi\n\n"
                        )
                    }

                    if (packet.PERMISSION && (!expectedPermFile.exists() || expectedPermFile.length() == 0L)) {
                        allRecovery.add("$MIGRATE_STATUS:perm:$packageName")
                    }
                }
            }

            return allRecovery
        }
        catch (e: Exception) {
            e.printStackTrace()
            allErrors.add("VERIFICATION_GENERATION${bd.errorTag}: ${e.message}")
            return null
        }
    }

    private fun correctBackups(defects: ArrayList<String>) {

        if (appBatch.appPackets.size == 0 || defects.size == 0) return

        actualBroadcast.apply {
            putExtra(EXTRA_PROGRESS_TYPE, EXTRA_PROGRESS_TYPE_CORRECTING)
            putExtra(DEFECT_NUMBER, 0)
            putExtra(EXTRA_PROGRESS_PERCENTAGE, 0)
        }

        val title = if (bd.totalParts > 1)
            engineContext.getString(R.string.correcting_errors) + " : " + madePartName
        else engineContext.getString(R.string.correcting_errors)

        commonTools.LBM?.sendBroadcast(actualBroadcast)

        try {
            val retryScript = File(engineContext.filesDir.absolutePath, "$FILE_PREFIX_RETRY_SCRIPT${bd.partNumber}.sh")
            val scriptWriter = BufferedWriter(FileWriter(retryScript))

            scriptWriter.write("#!sbin/sh\n\n")
            scriptWriter.write("echo \" \"\n")
            scriptWriter.write("sleep 1\n")
            scriptWriter.write("echo \"--- RECOVERY PID: $$\"\n")
            scriptWriter.write("cp ${retryScript.absolutePath} ${engineContext.externalCacheDir}/\n")

            for (i in 0 until defects.size){
                if (isBackupCancelled) break

                var defect = defects[i]
                if (defect.startsWith(MIGRATE_STATUS)){
                    val parts = defect.split(":")
                    if (parts.size == 3){
                        when (parts[1]){
                            "icon" -> {
                                val pi = pm.getPackageInfo(parts[2], 0)
                                backupUtils.makeIconFile(parts[2], backupUtils.getIconString(pi, pm), actualDestination)
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

                iterateBufferedReader(outputStream, {line ->
                    if (isCancelled) return@iterateBufferedReader true

                    if (line.startsWith("--- RECOVERY PID:")){
                        commonTools.tryIt {
                            VERIFICATION_PID = line.substring(line.lastIndexOf(" ") + 1).trim().toInt()
                        }
                    }
                    else if (line.startsWith("--- DEFECT:")){
                        val defectNumber = line.substring(line.lastIndexOf(" ") + 1).trim().toInt()
                        actualBroadcast.apply {
                            putExtra(DEFECT_NUMBER, defectNumber)
                            putExtra(EXTRA_PROGRESS_PERCENTAGE, commonTools.getPercentage(defectNumber, defects.size))
                        }
                    }

                    actualBroadcast.putExtra(EXTRA_RETRY_LOG, line)
                    commonTools.LBM?.sendBroadcast(actualBroadcast)

                    return@iterateBufferedReader line == "--- Retry complete ---"
                })

                commonTools.tryIt { it.waitFor() }

                iterateBufferedReader(errorStream, {errorLine ->

                    var ignorable = false

                    BackupUtils.ignorableWarnings.forEach {warnings ->
                        if (errorLine.endsWith(warnings)) ignorable = true
                    }

                    if (ignorable) allErrors.add("CORRECTION_ERR${bd.errorTag}: $errorLine")
                    else allErrors.add("CORRECTION_SUPPRESSED${bd.errorTag}: $errorLine")

                    return@iterateBufferedReader false
                }, null, false)
            }
        }
        catch (e: Exception){
            e.printStackTrace()
            allErrors.add("CORRECTION_TRY_CATCH${bd.errorTag}: ${e.message}")
        }
    }

    private fun cancelTask() {
        if (VERIFICATION_PID != -999) {
            commonTools.tryIt {
                val killProcess = Runtime.getRuntime().exec("su")

                val writer = BufferedWriter(OutputStreamWriter(killProcess.outputStream))
                writer.write("kill -9 $VERIFICATION_PID\n")
                writer.write("kill -15 $VERIFICATION_PID\n")
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

    override fun onPreExecute() {
        super.onPreExecute()
        backupDependencyComponent.inject(this)
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
        if (allErrors.size == 0)
            onBackupComplete.onBackupComplete(jobcode, true, bd.partNumber)
        else onBackupComplete.onBackupComplete(jobcode, false, allErrors)
    }

    override fun onCancelled() {
        super.onCancelled()
        isBackupCancelled = false
        cancelTask()
    }
}