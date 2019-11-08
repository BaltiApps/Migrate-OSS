package balti.migrate.backupEngines.engines

import balti.migrate.AppInstance.Companion.MAX_WORKING_SIZE
import balti.migrate.R
import balti.migrate.backupEngines.BackupServiceKotlin
import balti.migrate.backupEngines.ParentBackupClass
import balti.migrate.backupEngines.containers.BackupIntentData
import balti.migrate.backupEngines.containers.ZipAppBatch
import balti.migrate.backupEngines.containers.ZipAppPacket
import balti.migrate.extraBackupsActivity.apps.containers.AppPacket
import balti.migrate.utilities.CommonToolKotlin.Companion.ERR_MOVING
import balti.migrate.utilities.CommonToolKotlin.Companion.ERR_ZIP_ADDING_EXTRAS
import balti.migrate.utilities.CommonToolKotlin.Companion.ERR_ZIP_BATCHING
import balti.migrate.utilities.CommonToolKotlin.Companion.ERR_ZIP_PACKET_MAKING
import balti.migrate.utilities.CommonToolKotlin.Companion.EXTRA_PROGRESS_TYPE_MAKING_ZIP_BATCH
import balti.migrate.utilities.CommonToolKotlin.Companion.FILE_ZIP_NAME_EXTRAS
import balti.migrate.utilities.CommonToolKotlin.Companion.PREF_SEPARATE_EXTRAS_BACKUP
import balti.migrate.utilities.CommonToolKotlin.Companion.WARNING_ZIP_BATCH
import java.io.File

class MakeZipBatch(private val jobcode: Int, bd: BackupIntentData,
                   private val appList: ArrayList<AppPacket>, private val extras: ArrayList<File>) : ParentBackupClass(bd, EXTRA_PROGRESS_TYPE_MAKING_ZIP_BATCH) {

    private val zipBatches by lazy { ArrayList<ZipAppBatch>(0) }
    private val errors by lazy { ArrayList<String>(0) }
    private val warnings by lazy { ArrayList<String>(0) }
    private var totalSize = 0L

    private fun makeBatches(){

        var title = getTitle(R.string.making_packets)
        resetBroadcast(true, title)

        val allZipPackets = ArrayList<ZipAppPacket>(0)

        appList.forEach {

            try {

                if (BackupServiceKotlin.cancelAll) return

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

        val doSeparateExtras = sharedPreferences.getBoolean(PREF_SEPARATE_EXTRAS_BACKUP, true)

        try {

            var totalExtrasSize = 0L
            if (extras.isNotEmpty()) {
                extras.forEach {
                    totalExtrasSize += it.length()
                }
                totalExtrasSize /= 1024
            }

            var firstAdjust : Long =
            if (allZipPackets.isNotEmpty()) {
                // if no packets were made, a new packet with only extras will be made

                if (totalSize <= MAX_WORKING_SIZE) {
                    // this means only one zip batch will be created.
                    // So add extras size for adjustment
                    totalExtrasSize
                }
                else if (!doSeparateExtras) {
                    // this means more than one zip batch will be made
                    // But user has turned off separate extras.
                    // So add extras size for adjustment
                    totalExtrasSize
                }
                else 0
            }
            else 0

            while (allZipPackets.isNotEmpty()) {

                if (BackupServiceKotlin.cancelAll) return

                val zipAppBatchList = ArrayList<ZipAppPacket>(0)
                var batchSize = 0L
                var c = 0

                while (c < allZipPackets.size && batchSize < MAX_WORKING_SIZE - firstAdjust) {

                    val p = allZipPackets[c]
                    when {
                        p.zipPacketSize > MAX_WORKING_SIZE -> {
                            warnings.add("$WARNING_ZIP_BATCH: Removing, ${p.appPacket_z.appName}. Too big!")
                            allZipPackets.remove(p)
                        }
                        (batchSize + p.zipPacketSize) <= (MAX_WORKING_SIZE - firstAdjust) -> {
                            zipAppBatchList.add(p)
                            batchSize += p.zipPacketSize
                            allZipPackets.remove(p)
                        }
                        else -> c++
                    }
                }

                zipBatches.add(ZipAppBatch(zipAppBatchList))

                zipAppBatchList.clear()

                // size adjustment for extras to be made only for first batch. After that make it 0
                firstAdjust = 0
            }
        }
        catch (e: Exception){
            e.printStackTrace()
            errors.add("$ERR_ZIP_BATCHING: ${e.message}")
        }

        try {
            when (zipBatches.size) {
                0 -> {
                    if (extras.isNotEmpty())
                        zipBatches.add(ZipAppBatch().apply { addExtras(extras) })
                }
                1 -> {
                    zipBatches[0].addExtras(extras)
                }
                else -> {
                    var toBeAdded: ZipAppBatch? = null
                    if (doSeparateExtras) {
                        toBeAdded = ZipAppBatch().apply { addExtras(extras); partName = FILE_ZIP_NAME_EXTRAS }
                    } else zipBatches[0].apply {
                        addExtras(extras)
                    }

                    // update partNames
                    for (i in zipBatches.indices) {
                        val z = zipBatches[i]
                        z.partName = "${engineContext.getString(R.string.part)}_${i + 1}_${engineContext.getString(R.string.of)}_${zipBatches.size}"
                    }

                    if (toBeAdded != null) zipBatches.add(toBeAdded)
                }
            }
        }
        catch (e: Exception){
            e.printStackTrace()
            errors.add("$ERR_ZIP_ADDING_EXTRAS: ${e.message}")
        }
    }

    private fun movetoContainers() {

        val title = getTitle(R.string.moving_files)
        resetBroadcast(true, title)

        try {

            zipBatches.forEach {

                if (BackupServiceKotlin.cancelAll) return

                val fullDirToMoveTo: File

                if (it.partName != "") {

                    fullDirToMoveTo = File(actualDestination, it.partName)

                    fullDirToMoveTo.mkdirs()
                    val newFiles = ArrayList<File>(0)

                    // extras of each zipBatch
                    it.extrasFiles.forEach { ef ->

                        if (BackupServiceKotlin.cancelAll) return

                        val newFile = File(fullDirToMoveTo, ef.name)
                        ef.renameTo(newFile)
                        newFiles.add(newFile)
                    }
                    it.extrasFiles.clear()
                    it.extrasFiles.addAll(newFiles)

                    // app files of each zipPacket of each zipBatch
                    it.zipPackets.forEach {zp ->
                        newFiles.clear()
                        zp.appFiles.forEach {af ->

                            if (BackupServiceKotlin.cancelAll) return

                            val newFile = File(fullDirToMoveTo, af.name)
                            af.renameTo(newFile)
                            newFiles.add(newFile)
                        }
                        zp.appFiles.clear()
                        zp.appFiles.addAll(newFiles)
                    }
                }
                else fullDirToMoveTo = File(actualDestination)

                it.createFileList(fullDirToMoveTo.absolutePath)
            }

        }
        catch (e: Exception){
            e.printStackTrace()
            errors.add("$ERR_MOVING: ${e.message}")
        }
    }

    override fun doInBackground(vararg params: Any?): Any {

        makeBatches()
        if (errors.isEmpty()) movetoContainers()

        return 0
    }

    override fun postExecuteFunction() {
        if (errors.isEmpty()) {
            onEngineTaskComplete.onComplete(jobcode, errors, warnings, zipBatches)
        }
    }

}