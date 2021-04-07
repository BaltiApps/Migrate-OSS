package balti.migrate.backupEngines.engines

import android.util.Log
import balti.filex.FileX
import balti.filex.FileXInit
import balti.migrate.AppInstance.Companion.MAX_WORKING_SIZE
import balti.migrate.AppInstance.Companion.RESERVED_SPACE
import balti.migrate.R
import balti.migrate.backupEngines.BackupServiceKotlin
import balti.migrate.backupEngines.BackupServiceKotlin.Companion.flasherOnly
import balti.migrate.backupEngines.ParentBackupClass
import balti.migrate.backupEngines.containers.BackupIntentData
import balti.migrate.backupEngines.containers.ZipAppBatch
import balti.migrate.backupEngines.containers.ZipAppPacket
import balti.migrate.extraBackupsActivity.apps.containers.AppPacket
import balti.migrate.utilities.CommonToolsKotlin.Companion.DEBUG_TAG
import balti.migrate.utilities.CommonToolsKotlin.Companion.ERR_MOVING
import balti.migrate.utilities.CommonToolsKotlin.Companion.ERR_ZIP_ADDING_EXTRAS
import balti.migrate.utilities.CommonToolsKotlin.Companion.ERR_ZIP_BATCHING
import balti.migrate.utilities.CommonToolsKotlin.Companion.ERR_ZIP_PACKET_MAKING
import balti.migrate.utilities.CommonToolsKotlin.Companion.EXTRA_PROGRESS_TYPE_MAKING_ZIP_BATCH
import balti.migrate.utilities.CommonToolsKotlin.Companion.FILE_ZIP_NAME_EXTRAS
import balti.migrate.utilities.CommonToolsKotlin.Companion.PREF_FORCE_SEPARATE_EXTRAS_BACKUP
import balti.migrate.utilities.CommonToolsKotlin.Companion.PREF_NEW_ICON_METHOD
import balti.migrate.utilities.CommonToolsKotlin.Companion.PREF_SEPARATE_EXTRAS_BACKUP
import balti.migrate.utilities.CommonToolsKotlin.Companion.WARNING_ZIP_BATCH
import balti.module.baltitoolbox.functions.GetResources.getStringFromRes
import balti.module.baltitoolbox.functions.Misc
import balti.module.baltitoolbox.functions.Misc.getHumanReadableStorageSpace
import balti.module.baltitoolbox.functions.SharedPrefs.getPrefBoolean

class MakeZipBatch(private val jobcode: Int, bd: BackupIntentData,
                   private val appList: ArrayList<AppPacket>, private val extras: ArrayList<FileX>) : ParentBackupClass(bd, EXTRA_PROGRESS_TYPE_MAKING_ZIP_BATCH) {

    private val zipBatches by lazy { ArrayList<ZipAppBatch>(0) }
    private val errors by lazy { ArrayList<String>(0) }
    private val warnings by lazy { ArrayList<String>(0) }
    private var totalAppSize = 0L
    private var totalSizeConditionalExtras = 0L
    private var numberOfAppZipPackets = 0
    private var doSeparateExtras = false

    private val rootDir by lazy { FileX.new(actualDestination) }
    private val allFiles by lazy { rootDir.listEverything() }

    private fun shareProgress(string: String, progress : Int){
        Thread.sleep(50)
        broadcastProgress("", string, false, progress)
    }

    private fun getPercentage(allPacketsSize: Int): Int =
            Misc.getPercentage((numberOfAppZipPackets - allPacketsSize), numberOfAppZipPackets)

    private suspend fun makeBatches(){

        var title = getTitle(R.string.making_packets)

        val showNotification = !FileXInit.isTraditional
        resetBroadcast(!showNotification, title)

        val allAppZipPackets = ArrayList<ZipAppPacket>(0)

        val subTask: String = if (showNotification) getStringFromRes(R.string.reading_files_for_zip) else ""

        var c = 0
        appList.forEach {

            try {

                if (BackupServiceKotlin.cancelAll) return@forEach

                //val associatedFiles = ArrayList<FileX>(0)
                val associatedFileNames = ArrayList<String>(0)
                val associatedFileSizes = ArrayList<Long>(0)

                // old code
                /*val expectedAppDir = FileX.new(actualDestination, "${it.packageName}.app")
                val expectedDataFile = FileX.new(actualDestination, "${it.packageName}.tar.gz")
                val expectedPermFile = FileX.new(actualDestination, "${it.packageName}.perm")
                val expectedJsonFile = FileX.new(actualDestination, "${it.packageName}.json")
                val expectedSystemShFile = FileX.new(actualDestination, "${it.packageName}.sh")
                val expectedIconFile = FileX.new(actualDestination,
                        if (getPrefBoolean(PREF_NEW_ICON_METHOD, true)) "${it.packageName}.png"
                        else "${it.packageName}.icon"
                )*/

                if (showNotification){
                    val progress = Misc.getPercentage(++c, appList.size)
                    broadcastProgress(subTask, "${it.appName}(${c+1}/${appList.size})", true, progress)
                }

                val appDirName = "${it.packageName}.app"
                val dataName = "${it.packageName}.tar.gz"
                val permName = "${it.packageName}.perm"
                val jsonName = "${it.packageName}.json"
                val systemShName = "${it.packageName}.sh"
                val iconName = if (getPrefBoolean(PREF_NEW_ICON_METHOD, true)) "${it.packageName}.png"
                else "${it.packageName}.icon"

                // old code
                /*if (it.APP && expectedAppDir.exists()) associatedFiles.add(expectedAppDir)
                if (it.DATA && expectedDataFile.exists()) associatedFiles.add(expectedDataFile)
                if (it.PERMISSION && expectedPermFile.exists()) associatedFiles.add(expectedPermFile)
                if (expectedIconFile.exists()) associatedFiles.add(expectedIconFile)
                if (expectedJsonFile.exists()) associatedFiles.add(expectedJsonFile)
                if (expectedSystemShFile.exists()) associatedFiles.add(expectedSystemShFile)*/

                if (it.APP) {
                    FileX.new(actualDestination, appDirName).run {
                        refreshFile()
                        if (exists()) {
                            associatedFileNames.add(appDirName)
                            associatedFileSizes.add(getDirLength())
                        }
                    }
                }

                fun addToAssociatedFiles(name: String){
                    val index = allFiles?.first?.indexOf(name) ?: -1
                    if (index != -1) {
                        associatedFileNames.add(name)
                        associatedFileSizes.add(allFiles?.third?.get(index) ?: 0)
                    }
                }

                if (it.DATA) addToAssociatedFiles(dataName)
                if (it.PERMISSION) addToAssociatedFiles(permName)
                addToAssociatedFiles(jsonName)
                addToAssociatedFiles(systemShName)
                addToAssociatedFiles(iconName)

                val zipPacket = ZipAppPacket(it, associatedFileNames, associatedFileSizes)
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

            if (showNotification) getStringFromRes(R.string.reading_extras).let { broadcastProgress(it, it, true) }

            var totalExtrasSize = 0L
            if (extras.isNotEmpty()) {
                extras.forEach {
                    totalExtrasSize += it.length()
                }
            }

            if (showNotification) getStringFromRes(R.string.sorting).let { broadcastProgress(it, it, true) }

            Log.d(DEBUG_TAG, "total extras size: $totalExtrasSize")

            shareProgress("${engineContext.getString(R.string.total_extra_size)}: " +
                    "${getHumanReadableStorageSpace(totalExtrasSize)} ($totalExtrasSize B)", 0)

            doSeparateExtras =
                    if (flasherOnly){
                        totalExtrasSize > 0 && (getPrefBoolean(PREF_FORCE_SEPARATE_EXTRAS_BACKUP, false))
                    } else {
                        totalExtrasSize > 0
                                && (getPrefBoolean(PREF_FORCE_SEPARATE_EXTRAS_BACKUP, false)
                                || ((totalAppSize + totalExtrasSize) > (MAX_WORKING_SIZE - RESERVED_SPACE)
                                && getPrefBoolean(PREF_SEPARATE_EXTRAS_BACKUP, true)
                                ))
                    }

            var firstAdjust = // this is the adjustment size due to extras to be subtracted from first zip in the following cases:
                    when {
                        doSeparateExtras -> 0  // extras are completely separate. Hence no need for any adjustment.
                        else -> totalExtrasSize // no separate extras, hence adjust in the first zip
                    }

            Log.d(DEBUG_TAG, "first adjust size: $firstAdjust")

            if (!flasherOnly) (MAX_WORKING_SIZE - RESERVED_SPACE).run {

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
                        "${getHumanReadableStorageSpace(capSize)} ($capSize B)", 0)

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

            else {
                val zb = ArrayList<ZipAppPacket>(0)
                for (p in allAppZipPackets) zb.add(p)
                zipBatches.add(ZipAppBatch(zb))
            }
        }
        catch (e: Exception){
            e.printStackTrace()
            errors.add("$ERR_ZIP_BATCHING: ${e.message}")
        }

        try {

            // if separate extras, that is added later.
            // ELse, space for extras is already made in first zip by firstAdjust
            if (!doSeparateExtras && extras.isNotEmpty()) {
                if (zipBatches.isNotEmpty()) zipBatches[0].addExtras(extras)
                else zipBatches.add(ZipAppBatch().apply { addExtras(extras) })  // this is a case if only extras are being backed-up without any app
            }

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
        val showNotification = !FileXInit.isTraditional
        resetBroadcast(!showNotification, title)

        try {

            zipBatches.forEach {

                if (BackupServiceKotlin.cancelAll) return

                if (it.partName != "") {

                    val fullDirToMoveTo = FileX.new(actualDestination, it.partName)

                    if (showNotification) getStringFromRes(R.string.moving_extras).let { broadcastProgress(it, it, true, -1) }

                    fullDirToMoveTo.mkdirs()
                    val newFiles = ArrayList<FileX>(0)

                    // extras of each zipBatch
                    it.extrasFiles.forEach { ef ->

                        if (BackupServiceKotlin.cancelAll) return

                        val newFile = FileX.new(fullDirToMoveTo.path, ef.name)
                        ef.renameTo(newFile)
                        newFiles.add(newFile)
                    }
                    it.extrasFiles.clear()
                    it.extrasFiles.addAll(newFiles)

                    // app files of each zipPacket of each zipBatch
                    var c = 0
                    it.zipAppPackets.forEach { zp ->

                        if (showNotification) {
                            val progress = Misc.getPercentage(++c, it.zipAppPackets.size)
                            getStringFromRes(R.string.moving_app_files).let { broadcastProgress(it, it, true, progress) }
                        }

                        zp.appFileNames.forEach {name ->

                            if (BackupServiceKotlin.cancelAll) return

                            val newFile = FileX.new(fullDirToMoveTo.path, name)
                            val oldFile = FileX.new(actualDestination, name)
                            oldFile.renameTo(newFile)
                        }
                    }
                }

                it.createFileList(actualDestination)
            }

        }
        catch (e: Exception){
            e.printStackTrace()
            errors.add("$ERR_MOVING: ${e.message}")
        }
    }

    override suspend fun doInBackground(arg: Any?): Any? {

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