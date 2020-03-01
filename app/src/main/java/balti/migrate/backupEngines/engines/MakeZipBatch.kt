package balti.migrate.backupEngines.engines

import android.util.Log
import balti.migrate.AppInstance.Companion.MAX_WORKING_SIZE
import balti.migrate.AppInstance.Companion.RESERVED_SPACE
import balti.migrate.R
import balti.migrate.backupEngines.BackupServiceKotlin
import balti.migrate.backupEngines.ParentBackupClass
import balti.migrate.backupEngines.containers.BackupIntentData
import balti.migrate.backupEngines.containers.ZipAppBatch
import balti.migrate.backupEngines.containers.ZipAppPacket
import balti.migrate.extraBackupsActivity.apps.containers.AppPacket
import balti.migrate.utilities.CommonToolKotlin.Companion.DEBUG_TAG
import balti.migrate.utilities.CommonToolKotlin.Companion.ERR_MOVING
import balti.migrate.utilities.CommonToolKotlin.Companion.ERR_ZIP_ADDING_EXTRAS
import balti.migrate.utilities.CommonToolKotlin.Companion.ERR_ZIP_BATCHING
import balti.migrate.utilities.CommonToolKotlin.Companion.ERR_ZIP_PACKET_MAKING
import balti.migrate.utilities.CommonToolKotlin.Companion.EXTRA_PROGRESS_TYPE_MAKING_ZIP_BATCH
import balti.migrate.utilities.CommonToolKotlin.Companion.FILE_ZIP_NAME_EXTRAS
import balti.migrate.utilities.CommonToolKotlin.Companion.PREF_FORCE_SEPARATE_EXTRAS_BACKUP
import balti.migrate.utilities.CommonToolKotlin.Companion.PREF_SEPARATE_EXTRAS_BACKUP
import balti.migrate.utilities.CommonToolKotlin.Companion.WARNING_ZIP_BATCH
import java.io.File

class MakeZipBatch(private val jobcode: Int, bd: BackupIntentData,
                   private val appList: ArrayList<AppPacket>, private val extras: ArrayList<File>) : ParentBackupClass(bd, EXTRA_PROGRESS_TYPE_MAKING_ZIP_BATCH) {

    private val zipBatches by lazy { ArrayList<ZipAppBatch>(0) }
    private val errors by lazy { ArrayList<String>(0) }
    private val warnings by lazy { ArrayList<String>(0) }
    private var totalAppSize = 0L
    private var totalSizeConditionalExtras = 0L
    private var numberOfAppZipPackets = 0
    private var doSeparateExtras = false

    private fun shareProgress(string: String, progress : Int){
        Thread.sleep(50)
        broadcastProgress("", string, false, progress)
    }

    private fun getPercentage(allPacketsSize: Int): Int =
            commonTools.getPercentage((numberOfAppZipPackets - allPacketsSize), numberOfAppZipPackets)

    private fun makeBatches(){

        var title = getTitle(R.string.making_packets)
        resetBroadcast(true, title)

        val allAppZipPackets = ArrayList<ZipAppPacket>(0)

        appList.forEach {

            try {

                if (BackupServiceKotlin.cancelAll) return@forEach

                val associatedFiles = ArrayList<File>(0)

                val expectedAppDir = File(actualDestination, "${it.packageName}.app")
                val expectedDataFile = File(actualDestination, "${it.packageName}.tar.gz")
                val expectedPermFile = File(actualDestination, "${it.packageName}.perm")
                val expectedJsonFile = File(actualDestination, "${it.packageName}.json")
                val expectedIconFile = File(actualDestination, "${it.packageName}.icon")

                if (it.APP && expectedAppDir.exists()) associatedFiles.add(expectedAppDir)
                if (it.DATA && expectedDataFile.exists()) associatedFiles.add(expectedDataFile)
                if (it.PERMISSION && expectedPermFile.exists()) associatedFiles.add(expectedPermFile)
                if (expectedIconFile.exists()) associatedFiles.add(expectedIconFile)
                if (expectedJsonFile.exists()) associatedFiles.add(expectedJsonFile)

                val zipPacket = ZipAppPacket(it, associatedFiles)
                allAppZipPackets.add(zipPacket)
                totalAppSize += zipPacket.zipPacketSize
            }
            catch (e: Exception){
                e.printStackTrace()
                errors.add("$ERR_ZIP_PACKET_MAKING: ${e.message}")
            }
        }

        Thread.sleep(100)

        title = getTitle(R.string.making_batches)
        resetBroadcast(false, title)

        try {

            var totalExtrasSize = 0L
            if (extras.isNotEmpty()) {
                extras.forEach {
                    totalExtrasSize += it.length()
                }
            }

            Log.d(DEBUG_TAG, "total extras size: $totalExtrasSize")

            shareProgress("${engineContext.getString(R.string.total_extra_size)}: " +
                    "${commonTools.getHumanReadableStorageSpace(totalExtrasSize)} ($totalExtrasSize B)", 0)

            doSeparateExtras = totalExtrasSize > 0
                    && (sharedPreferences.getBoolean(PREF_FORCE_SEPARATE_EXTRAS_BACKUP, false)
                        || ((totalAppSize + totalExtrasSize) > (MAX_WORKING_SIZE - RESERVED_SPACE)
                        && sharedPreferences.getBoolean(PREF_SEPARATE_EXTRAS_BACKUP, true)
                        )
                    )

            var firstAdjust = // this is the adjustment size due to extras to be subtracted from first zip in the following cases:
                    when {
                        doSeparateExtras -> 0  // extras are completely separate. Hence no need for any adjustment.
                        else -> totalExtrasSize // no separate extras, hence adjust in the first zip
                    }

            Log.d(DEBUG_TAG, "first adjust size: $firstAdjust")

            (MAX_WORKING_SIZE - RESERVED_SPACE).run {

                val localMax = (this * 0.8).toLong()
                // Initially set a cap of 80% of max.
                // Apps which will not fit here, but within allowed size
                // will get zips of their own.

                totalSizeConditionalExtras = (totalAppSize + if (doSeparateExtras) 0 else totalExtrasSize)
                    // this is the total including apps + extras if extras are not separate

                Log.d(DEBUG_TAG, "totalSizeConditionalExtras: $totalSizeConditionalExtras")
                Log.d(DEBUG_TAG, "totalAppSize: $totalAppSize")

                val parts = (totalSizeConditionalExtras / localMax) + 1
                /* Examples: assume
                 * max size = 4 GB, totalSize = 10 kb
                 *    parts = (0 by decimal division) + 1 = 1
                 * max size = 4 GB, totalSize = 4 GB
                 *    parts = (1 by decimal division) + 1 = 2  => Backup will be split into two parts of 2 GB
                 * max size = 4 GB, totalSize = 4.1 GB
                 *    parts = (1 by decimal division) + 1 = 2  => Backup will be split into two parts of greater than 2 GB
                 */

                shareProgress("${engineContext.getString(R.string.parts)}: $parts", 0)

                val capSize = totalSizeConditionalExtras / parts
                shareProgress("${engineContext.getString(R.string.capping_size)}: " +
                        "${commonTools.getHumanReadableStorageSpace(capSize)} ($capSize B)", 0)

                // remove all apps which are too big
                var c = 0
                while (c < allAppZipPackets.size){
                    val p = allAppZipPackets[c]
                    if (p.zipPacketSize > this) {
                        warnings.add("$WARNING_ZIP_BATCH: Removing ${p.appPacket_z.appName}. Cannot backup. Too big!")
                        allAppZipPackets.remove(p)
                        --c
                    }
                    ++c
                }

                numberOfAppZipPackets = allAppZipPackets.size

                var maxOuterLoop = 1000   // break if outerloop has scanned over this number.

                while (allAppZipPackets.isNotEmpty() && maxOuterLoop > 0) {
                    // all eligible apps must be put in packets

                    if (BackupServiceKotlin.cancelAll) return

                    val zipAppBatchList = ArrayList<ZipAppPacket>(0)
                    var batchSize = 0L
                    c = 0

                    // initially add small apps within 80% max size
                    while (c < allAppZipPackets.size && batchSize < (capSize - firstAdjust)) {

                        val p = allAppZipPackets[c]

                        if ((batchSize + p.zipPacketSize) <= (capSize - firstAdjust)) {
                            zipAppBatchList.add(p)
                            batchSize += p.zipPacketSize
                            allAppZipPackets.remove(p)
                            shareProgress("${engineContext.getString(R.string.adding)}: ${p.appPacket_z.appName} | " +
                                    "${engineContext.getString(R.string.packet)}: ${zipBatches.size + 1}", getPercentage(allAppZipPackets.size))
                        } else c++
                    }

                    if (firstAdjust == 0L && zipAppBatchList.isEmpty()) break
                    // firstAdjust will be always zero if separate extras,
                        // in that case if zipAppBatchList is empty, none of the apps none can be satisfied with the 20% reduced cap size
                    // firstAdjust will not be zero if not separate extras, only in the first iteration,
                        // in that case do not break even if zipAppBatchList is empty.
                    // firstAdjust will be zero after first iteration
                        // in that case if zipAppBatchList is empty, none of the apps none can be satisfied with the 20% reduced cap size

                    else zipBatches.add(ZipAppBatch(zipAppBatchList))
                    // if zipAppBatchList is empty but firstAdjust not 0, an empty batch is added for accommodating only extras

                    firstAdjust = 0
                    --maxOuterLoop
                }

                // make dedicated batches for bigger apps needing full max size
                maxOuterLoop = 1000
                while (allAppZipPackets.isNotEmpty() && maxOuterLoop > 0) {

                    if (BackupServiceKotlin.cancelAll) return
                    c = 0

                    val p = allAppZipPackets[c]

                    if (p.zipPacketSize <= (this)) {
                        zipBatches.add(ZipAppBatch(arrayListOf(p)))
                        shareProgress("${engineContext.getString(R.string.adding_bigger_apps)}: ${p.appPacket_z.appName}",
                                getPercentage(allAppZipPackets.size))
                        allAppZipPackets.remove(p)
                    }
                    else ++c

                    --maxOuterLoop
                }

                // show errors for packets could not be added
                allAppZipPackets.forEach {
                    errors.add("$ERR_ZIP_BATCHING: Could not batch, ${it.appPacket_z.appName}.")
                }

            }
        }
        catch (e: Exception){
            e.printStackTrace()
            errors.add("$ERR_ZIP_BATCHING: ${e.message}")
        }

        try {
            /*when (zipBatches.size) {
                0 -> {
                    if (extras.isNotEmpty())
                        zipBatches.add(ZipAppBatch().apply { addExtras(extras) })
                }
                1 -> {
                    //zipBatches[0].apply { partName = "TEST_PART" }.addExtras(extras)
                    zipBatches[0].addExtras(extras)
                }
                else -> {
                    var toBeAdded: ZipAppBatch? = null

                    if (extras.isNotEmpty()) {
                        if (doSeparateExtras) {
                            toBeAdded = ZipAppBatch().apply { addExtras(extras); partName = FILE_ZIP_NAME_EXTRAS }
                        } else zipBatches[0].apply {
                            addExtras(extras)
                        }
                    }

                    // update partNames
                    for (i in zipBatches.indices) {
                        val z = zipBatches[i]
                        z.partName = "${engineContext.getString(R.string.part)}_${i + 1}_${engineContext.getString(R.string.of)}_${zipBatches.size}"
                    }

                    if (toBeAdded != null) zipBatches.add(toBeAdded)
                }
            }*/

            // if separate extras, that is added later.
            // ELse, space for extras is already made in first zip by firstAdjust
            if (!doSeparateExtras && extras.isNotEmpty() && zipBatches.isNotEmpty()) zipBatches[0].addExtras(extras)

            // update partNames
            // later, depending on partName containers are made.
            // if partName is empty, means no container. This is the case for only one final zip.
            // hence update partNames only if multiple zip batches and/or separate extras
            if (zipBatches.size > 1 || doSeparateExtras)
                for (i in zipBatches.indices) {
                    val z = zipBatches[i]
                    z.partName = "${engineContext.getString(R.string.part)}_${i + 1}_${engineContext.getString(R.string.of)}_${zipBatches.size}"
                }

            if (doSeparateExtras) zipBatches.add(ZipAppBatch().apply { addExtras(extras); partName = FILE_ZIP_NAME_EXTRAS })
            // add a zipBatch if extras are separate

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

                it.createFileList(File(actualDestination).absolutePath)
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