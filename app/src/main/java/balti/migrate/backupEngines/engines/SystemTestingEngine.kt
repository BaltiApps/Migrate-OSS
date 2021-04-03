package balti.migrate.backupEngines.engines

import balti.filex.FileX
import balti.migrate.R
import balti.migrate.backupEngines.BackupServiceKotlin
import balti.migrate.backupEngines.ParentBackupClass
import balti.migrate.backupEngines.containers.BackupIntentData
import balti.migrate.backupEngines.utils.BackupUtils
import balti.migrate.utilities.CommonToolsKotlin.Companion.ERR_TESTING_ERROR
import balti.migrate.utilities.CommonToolsKotlin.Companion.ERR_TESTING_TRY_CATCH
import balti.migrate.utilities.CommonToolsKotlin.Companion.EXTRA_PROGRESS_TYPE_TESTING
import balti.migrate.utilities.CommonToolsKotlin.Companion.PREF_RETRY_SYSTEM_CHECK
import balti.module.baltitoolbox.functions.FileHandlers.unpackAssetToInternal
import balti.module.baltitoolbox.functions.GetResources.getStringFromRes
import balti.module.baltitoolbox.functions.Misc.tryIt
import balti.module.baltitoolbox.functions.SharedPrefs.getPrefBoolean
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter

class SystemTestingEngine(private val jobcode: Int, private val bd: BackupIntentData,
                          private val busyboxBinaryPath: String) : ParentBackupClass(bd, EXTRA_PROGRESS_TYPE_TESTING) {

    private val pm by lazy { engineContext.packageManager }
    private val backupUtils by lazy { BackupUtils() }
    private var suProcess : Process? = null

    private var TESTING_PID = -999

    private val errors by lazy { ArrayList<String>(0) }

    override suspend fun doInBackground(arg: Any?): Any? {

        fun test() {
            try {

                val title = getTitle(R.string.testing_system)

                resetBroadcast(true, title)

                val testScriptPath = unpackAssetToInternal("systemTestScript.sh", "test.sh")
                val thisPackageInfo = pm.getApplicationInfo(engineContext.packageName, 0)

                val rootLocation = FileX.new(actualDestination).apply { mkdirs() }.canonicalPath
                val expectedApkFile = FileX.new(actualDestination, "${thisPackageInfo.packageName}.apk")
                val expectedDataFile = FileX.new(actualDestination, "${thisPackageInfo.packageName}.tar.gz")
                expectedApkFile.run { if (exists()) delete() }
                expectedDataFile.run { if (exists()) delete() }

                val dataPathDirPath = thisPackageInfo.dataDir.let {
                    if (it.endsWith("/")) it.substring(0, it.length - 1)
                    else it
                }
                val dataPath = dataPathDirPath.substring(0, dataPathDirPath.lastIndexOf("/"))
                val dataName = dataPathDirPath.substring(dataPathDirPath.lastIndexOf("/") + 1)

                suProcess = Runtime.getRuntime().exec("su")
                suProcess?.let {
                    val suWriter = BufferedWriter(OutputStreamWriter(it.outputStream))
                    suWriter.write("sh $testScriptPath ${thisPackageInfo.packageName} ${thisPackageInfo.sourceDir} $dataPath $dataName $rootLocation $busyboxBinaryPath\n")
                    suWriter.write("exit\n")
                    suWriter.flush()

                    val resultStream = BufferedReader(InputStreamReader(it.inputStream))

                    backupUtils.iterateBufferedReader(resultStream, { line ->

                        if (BackupServiceKotlin.cancelAll) {
                            cancelTask(suProcess, TESTING_PID)
                            return@iterateBufferedReader true
                        }

                        if (line.startsWith("--- PID:")) {
                            tryIt { TESTING_PID = line.substring(line.lastIndexOf(" ") + 1).toInt() }
                        }

                        broadcastProgress("", line, false)

                        return@iterateBufferedReader line == "--- Test done ---"

                    })

                    val errorStream = BufferedReader(InputStreamReader(it.errorStream))

                    backupUtils.iterateBufferedReader(errorStream, { errorLine ->

                        var ignorable = false

                        BackupUtils.ignorableWarnings.forEach { warnings ->
                            if (errorLine.endsWith(warnings)) ignorable = true
                        }

                        if (!ignorable) errors.add("$ERR_TESTING_ERROR: $errorLine")
                        return@iterateBufferedReader false
                    })

                    expectedApkFile.refreshFile()
                    expectedDataFile.refreshFile()

                    if (!expectedApkFile.exists() || expectedApkFile.length() == 0L)
                        errors.add(engineContext.getString(R.string.test_apk_not_found))
                    else expectedApkFile.delete()

                    if (!expectedDataFile.exists() || expectedDataFile.length() == 0L)
                        errors.add(engineContext.getString(R.string.test_data_not_found))
                    else expectedDataFile.delete()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                errors.add("$ERR_TESTING_TRY_CATCH: ${e.message}")
            }
        }

        test()
        if (errors.isNotEmpty()){
            if (getPrefBoolean(PREF_RETRY_SYSTEM_CHECK, true)){
                errors.clear()
                resetBroadcast(true, getTitle(R.string.retrying_system_test))
                getStringFromRes(R.string.retrying_after_5sec).let {broadcastProgress(it, it, true)}
                sleepTask(5000)
                test()
            }
        }
        return 0

    }

    override fun postExecuteFunction() {
        onEngineTaskComplete.onComplete(jobcode, errors)
    }
}