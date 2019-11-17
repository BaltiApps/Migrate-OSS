package balti.migrate.backupEngines.engines

import balti.migrate.R
import balti.migrate.backupEngines.BackupServiceKotlin
import balti.migrate.backupEngines.ParentBackupClass
import balti.migrate.backupEngines.containers.BackupIntentData
import balti.migrate.backupEngines.utils.BackupUtils
import balti.migrate.utilities.CommonToolKotlin.Companion.ERR_TESTING_ERROR
import balti.migrate.utilities.CommonToolKotlin.Companion.ERR_TESTING_TRY_CATCH
import balti.migrate.utilities.CommonToolKotlin.Companion.EXTRA_PROGRESS_TYPE_TESTING
import java.io.*

class SystemTestingEngine(private val jobcode: Int, private val bd: BackupIntentData,
                          private val busyboxBinaryPath: String) : ParentBackupClass(bd, EXTRA_PROGRESS_TYPE_TESTING) {

    private val pm by lazy { engineContext.packageManager }
    private val backupUtils by lazy { BackupUtils() }
    private var suProcess : Process? = null

    private var TESTING_PID = -999

    private val errors by lazy { ArrayList<String>(0) }

    override fun doInBackground(vararg params: Any?): Any {

        try {

            val title = getTitle(R.string.testing_system)

            resetBroadcast(true, title)

            val testScriptPath = commonTools.unpackAssetToInternal("systemTestScript.sh", "test.sh", false)
            val thisPackageInfo = pm.getApplicationInfo(engineContext.packageName, 0)

            val dataPathDirPath = thisPackageInfo.dataDir.let {
                if (it.endsWith("/")) it.substring(0, it.length - 1)
                else it
            }
            val dataPath = dataPathDirPath.substring(0, dataPathDirPath.lastIndexOf("/"))
            val dataName = dataPathDirPath.substring(dataPathDirPath.lastIndexOf("/")+1)

            suProcess = Runtime.getRuntime().exec("su")
            suProcess?.let {
                val suWriter = BufferedWriter(OutputStreamWriter(it.outputStream))
                suWriter.write("sh $testScriptPath ${thisPackageInfo.packageName} ${thisPackageInfo.sourceDir} $dataPath $dataName ${engineContext.externalCacheDir.absolutePath} $busyboxBinaryPath\n")
                suWriter.write("exit\n")
                suWriter.flush()

                val resultStream = BufferedReader(InputStreamReader(it.inputStream))

                backupUtils.iterateBufferedReader(resultStream, { line ->

                    if (BackupServiceKotlin.cancelAll) {
                        cancelTask(suProcess, TESTING_PID)
                        return@iterateBufferedReader true
                    }

                    if (line.startsWith("--- PID:")) {
                        commonTools.tryIt { TESTING_PID = line.substring(line.lastIndexOf(" ") + 1).toInt() }
                    }

                    broadcastProgress("", line, false)

                    return@iterateBufferedReader line == "--- Test done ---"

                })

                val errorStream = BufferedReader(InputStreamReader(it.errorStream))

                backupUtils.iterateBufferedReader(errorStream, { errorLine ->

                    var ignorable = false

                    BackupUtils.ignorableWarnings.forEach {warnings ->
                        if (errorLine.endsWith(warnings)) ignorable = true
                    }

                    if (!ignorable) errors.add("$ERR_TESTING_ERROR: $errorLine")
                    return@iterateBufferedReader false
                })

                val expectedApkFile = File(engineContext.externalCacheDir, "${thisPackageInfo.packageName}.apk")
                val expectedDataFile = File(engineContext.externalCacheDir, "${thisPackageInfo.packageName}.tar.gz")

                if (!expectedApkFile.exists() || expectedApkFile.length() == 0L)
                    errors.add(engineContext.getString(R.string.test_apk_not_found))
                else expectedApkFile.delete()

                if (!expectedDataFile.exists() || expectedDataFile.length() == 0L)
                    errors.add(engineContext.getString(R.string.test_data_not_found))
                else expectedDataFile.delete()
            }
        }
        catch (e: Exception) {
            e.printStackTrace()
            errors.add("$ERR_TESTING_TRY_CATCH: ${e.message}")
        }

        return 0

    }

    override fun postExecuteFunction() {
        onEngineTaskComplete.onComplete(jobcode, errors)
    }
}