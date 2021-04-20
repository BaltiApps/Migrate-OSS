package balti.migrate.backupEngines.engines

import balti.filex.FileX
import balti.migrate.R
import balti.migrate.backupEngines.BackupServiceKotlin
import balti.migrate.backupEngines.ParentBackupClass
import balti.migrate.backupEngines.containers.BackupIntentData
import balti.migrate.extraBackupsActivity.wifi.containers.WifiDataPacket
import balti.migrate.utilities.CommonToolsKotlin.Companion.ERR_WIFI_TRY_CATCH
import balti.migrate.utilities.CommonToolsKotlin.Companion.EXTRA_PROGRESS_TYPE_WIFI

class WifiBackupEngine(private val jobcode: Int,
                       private val bd: BackupIntentData,
                       private val wifiDataPacket: WifiDataPacket) : ParentBackupClass(bd, EXTRA_PROGRESS_TYPE_WIFI) {

    private val wifiFile by lazy { FileX.new(actualDestination, wifiDataPacket.fileName) }
    private val errors by lazy { ArrayList<String>(0) }

    override suspend fun doInBackground(arg: Any?): Any? {

        try {

            FileX.new(actualDestination).mkdirs()

            val title = getTitle(R.string.storing_wifi_data)
            wifiFile.createNewFile(overwriteIfExists = true)
            wifiFile.refreshFile()

            resetBroadcast(true, title)

            heavyTask {
                wifiFile.startWriting(object : FileX.Writer(){
                    override fun writeLines() {
                        wifiDataPacket.contents.let { contents ->
                            for (i in 0 until contents.size) {
                                if (BackupServiceKotlin.cancelAll) break
                                write(contents[i])
                            }
                        }
                    }
                })
            }
        }
        catch (e: Exception){
            e.printStackTrace()
            errors.add("$ERR_WIFI_TRY_CATCH: ${e.message}")
        }

        return 0
    }

    override fun postExecuteFunction() {
        onEngineTaskComplete.onComplete(jobcode, errors, jobResults = arrayOf(wifiFile))
    }
}