package balti.migrate.backupEngines.engines

import android.widget.Toast
import balti.migrate.R
import balti.migrate.backupEngines.ParentBackupClass
import balti.migrate.backupEngines.containers.BackupIntentData
import balti.migrate.backupEngines.utils.BackupUtils
import balti.migrate.utilities.CommonToolKotlin.Companion.ERR_TESTING_ERROR
import balti.migrate.utilities.CommonToolKotlin.Companion.ERR_TESTING_TRY_CATCH
import balti.migrate.utilities.CommonToolKotlin.Companion.EXTRA_PROGRESS_TYPE_TESTING
import balti.migrate.utilities.CommonToolKotlin.Companion.EXTRA_TEST_LOG
import balti.migrate.utilities.CommonToolKotlin.Companion.EXTRA_TITLE
import java.io.*

class SystemTestingEngine(private val jobcode: Int, private val bd: BackupIntentData,
                          private val busyboxBinaryPath: String) : ParentBackupClass(bd, EXTRA_PROGRESS_TYPE_TESTING) {

    private val pm by lazy { engineContext.packageManager }
    private val backupUtils by lazy { BackupUtils() }
    private var suProcess : Process? = null

    private var TESTING_PID = -999

    private val testingErrors by lazy { ArrayList<String>(0) }

    init {
        customCancelFunction = { commonTools.tryIt { cancelTask() } }
    }

    override fun doInBackground(vararg params: Any?): Any {

        try {

            val title = if (bd.totalParts > 1)
                engineContext.getString(R.string.testing_system) + " : " + madePartName
            else engineContext.getString(R.string.testing_system)

            actualBroadcast.putExtra(EXTRA_TITLE, title)
            broadcastProgress()

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

                    if (isBackupCancelled) return@iterateBufferedReader true

                    if (line.startsWith("--- PID:")) {
                        commonTools.tryIt { TESTING_PID = line.substring(line.lastIndexOf(" ") + 1).toInt() }
                    }

                    actualBroadcast.putExtra(EXTRA_TEST_LOG, line)
                    broadcastProgress()

                    return@iterateBufferedReader line == "--- Test done ---"

                })

                val errorStream = BufferedReader(InputStreamReader(it.errorStream))

                backupUtils.iterateBufferedReader(errorStream, { errorLine ->

                    var ignorable = false

                    BackupUtils.ignorableWarnings.forEach {warnings ->
                        if (errorLine.endsWith(warnings)) ignorable = true
                    }

                    if (!ignorable) testingErrors.add("$ERR_TESTING_ERROR${bd.errorTag}: $errorLine")
                    return@iterateBufferedReader false
                })

                val expectedApkFile = File(engineContext.externalCacheDir, "${thisPackageInfo.packageName}.apk")
                val expectedDataFile = File(engineContext.externalCacheDir, "${thisPackageInfo.packageName}.tar.gz")

                if (!expectedApkFile.exists() || expectedApkFile.length() == 0L)
                    testingErrors.add(engineContext.getString(R.string.test_apk_not_found))
                if (!expectedDataFile.exists() || expectedDataFile.length() == 0L)
                    testingErrors.add(engineContext.getString(R.string.test_data_not_found))
            }
        }
        catch (e: Exception) {
            e.printStackTrace()
            testingErrors.add("$ERR_TESTING_TRY_CATCH${bd.errorTag}: ${e.message}")
        }

        return 0

    }

    private fun cancelTask() {
        if (TESTING_PID != -999) {
            commonTools.tryIt {
                val killProcess = Runtime.getRuntime().exec("su")

                val writer = BufferedWriter(OutputStreamWriter(killProcess.outputStream))
                writer.write("kill -9 $TESTING_PID\n")
                writer.write("kill -15 $TESTING_PID\n")
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

    override fun onPostExecute(result: Any?) {
        super.onPostExecute(result)
        onBackupComplete.onBackupComplete(jobcode, testingErrors.size == 0, testingErrors)
    }
}