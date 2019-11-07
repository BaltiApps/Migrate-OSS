package balti.migrate.backupEngines.engines

import balti.migrate.AppInstance.Companion.MAX_WORKING_SIZE
import balti.migrate.R
import balti.migrate.backupEngines.BackupServiceKotlin
import balti.migrate.backupEngines.ParentBackupClass
import balti.migrate.backupEngines.containers.BackupIntentData
import balti.migrate.backupEngines.containers.ZipAppBatch
import balti.migrate.backupEngines.containers.ZipAppPacket
import balti.migrate.extraBackupsActivity.apps.containers.AppPacket
import balti.migrate.utilities.CommonToolKotlin.Companion.ERR_ZIP_BATCHING
import balti.migrate.utilities.CommonToolKotlin.Companion.ERR_ZIP_PACKET_MAKING
import balti.migrate.utilities.CommonToolKotlin.Companion.EXTRA_PROGRESS_TYPE_MAKING_ZIP_BATCH
import balti.migrate.utilities.CommonToolKotlin.Companion.WARNING_ZIP_BATCH
import java.io.File

class MakeZipBatch(private val jobcode: Int, private val bd: BackupIntentData,
                   private val appList: ArrayList<AppPacket>) : ParentBackupClass(bd, EXTRA_PROGRESS_TYPE_MAKING_ZIP_BATCH) {

    private val zipBatches by lazy { ArrayList<ZipAppBatch>(0) }
    private val errors by lazy { ArrayList<String>(0) }
    private val warnings by lazy { ArrayList<String>(0) }
    private var totalSize = 0L

    override fun doInBackground(vararg params: Any?): Any {

        var title = getTitle(R.string.making_packets)
        resetBroadcast(true, title)

        val allZipPackets = ArrayList<ZipAppPacket>(0)

        appList.forEach {

            try {

                if (BackupServiceKotlin.cancelAll) return 0

                val associatedFiles = ArrayList<File>(0)

                val expectedAppDir = File(actualDestination, "${it.packageName}.app")
                val expectedDataFile = File(actualDestination, it.dataName)
                val expectedPermFile = File(actualDestination, "${it.packageName}.perm")
                val expectedJsonFile = File(actualDestination, "${it.packageName}.json")
                val expectedIconFile = File(actualDestination, "${it.packageName}.icon")

                if (it.APP && expectedAppDir.exists()) associatedFiles.add(expectedAppDir)
                if (it.DATA && expectedDataFile.exists()) associatedFiles.add(expectedDataFile)
                if (it.PERMISSION && expectedPermFile.exists()) associatedFiles.add(expectedPermFile)
                if (expectedIconFile.exists()) associatedFiles.add(expectedIconFile)
                if (expectedJsonFile.exists()) associatedFiles.add(expectedJsonFile)

                val zipPacket = ZipAppPacket(it, associatedFiles)
                allZipPackets.add(zipPacket)
                totalSize += zipPacket.zipPacketSize
            }
            catch (e: Exception){
                e.printStackTrace()
                errors.add("$ERR_ZIP_PACKET_MAKING: ${e.message}")
            }
        }

        title = getTitle(R.string.making_batches)
        resetBroadcast(true, title)

        try {

            while (allZipPackets.isNotEmpty()) {

                if (BackupServiceKotlin.cancelAll) return 0

                val zipAppBatchList = ArrayList<ZipAppPacket>(0)
                var batchSize = 0L
                var c = 0

                while (c < allZipPackets.size && batchSize < MAX_WORKING_SIZE) {

                    val p = allZipPackets[c]
                    when {
                        p.zipPacketSize > MAX_WORKING_SIZE -> {
                            warnings.add("$WARNING_ZIP_BATCH: Removing, ${p.appPacket_z.appName}. Too big!")
                            allZipPackets.remove(p)
                        }
                        (batchSize + p.zipPacketSize) <= MAX_WORKING_SIZE -> {
                            zipAppBatchList.add(p)
                            batchSize += p.zipPacketSize
                            allZipPackets.remove(p)
                        }
                        else -> c++
                    }
                }

            }
        }
        catch (e: Exception){
            e.printStackTrace()
            errors.add("$ERR_ZIP_BATCHING: ${e.message}")
        }

        return 0
    }

    override fun postExecuteFunction() {
        if (errors.isEmpty()) {
            onEngineTaskComplete.onComplete(jobcode, errors, warnings, zipBatches)
        }
    }

}