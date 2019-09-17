package balti.migrate.backupEngines.engines

import balti.migrate.R
import balti.migrate.backupEngines.ParentBackupClass
import balti.migrate.backupEngines.containers.BackupIntentData
import balti.migrate.extraBackupsActivity.wifi.containers.WifiDataPacket
import balti.migrate.utilities.CommonToolKotlin.Companion.ERR_WIFI_TRY_CATCH
import balti.migrate.utilities.CommonToolKotlin.Companion.EXTRA_PROGRESS_PERCENTAGE
import balti.migrate.utilities.CommonToolKotlin.Companion.EXTRA_PROGRESS_TYPE_WIFI
import balti.migrate.utilities.CommonToolKotlin.Companion.EXTRA_TITLE
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter

class WifiBackupEngine(private val jobcode: Int,
                       private val bd: BackupIntentData,
                       private val wifiDataPacket: WifiDataPacket) : ParentBackupClass(bd, EXTRA_PROGRESS_TYPE_WIFI) {

    private val wifiFile by lazy { File(actualDestination, wifiDataPacket.fileName) }
    private val errors by lazy { ArrayList<String>(0) }

    override fun doInBackground(vararg params: Any?): Any {

        try {

            File(actualDestination).mkdirs()
            if (wifiFile.exists()) wifiFile.delete()

            val title = if (bd.totalParts > 1)
                engineContext.getString(R.string.storing_wifi_data) + " : " + madePartName
            else engineContext.getString(R.string.storing_wifi_data)

            actualBroadcast.apply {
                putExtra(EXTRA_TITLE, title)
                putExtra(EXTRA_PROGRESS_PERCENTAGE, 0)
            }
            commonTools.LBM?.sendBroadcast(actualBroadcast)

            BufferedWriter(FileWriter(wifiFile, true)).run {

                wifiDataPacket.contents.let {contents ->
                    for (i in 0 until contents.size) {

                        if (isBackupCancelled) break

                        this.write(contents[i])

                        actualBroadcast.putExtra(EXTRA_PROGRESS_PERCENTAGE,
                                commonTools.getPercentage((i + 1), contents.size))

                        commonTools.LBM?.sendBroadcast(actualBroadcast)

                    }
                }

                this.close()
            }

            BufferedWriter(FileWriter(File(actualDestination, "fileList.txt"), true)).run {
                this.write("${wifiDataPacket.fileName}\n")
                this.close()
            }
        }
        catch (e: Exception){
            e.printStackTrace()
            errors.add("$ERR_WIFI_TRY_CATCH${bd.errorTag}: ${e.message}")
        }

        return 0
    }

    override fun onPostExecute(result: Any?) {
        super.onPostExecute(result)
        if (errors.size == 0)
            onBackupComplete.onBackupComplete(jobcode, true, 0)
        else onBackupComplete.onBackupComplete(jobcode, false, errors)
    }
}