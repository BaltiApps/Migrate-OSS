package balti.migrate.backupEngines.containers

import balti.filex.FileX
import balti.migrate.extraBackupsActivity.apps.containers.AppPacket

/**
 * A container to store AppPacket with file sizes. Created in [balti.migrate.backupEngines.engines.MakeZipBatch.makeZipPackets].
 * This is used to sort apps in 4GB (or lower) flashable zip files in [balti.migrate.backupEngines.engines.MakeZipBatch.makeBatches].
 *
 * For AppPacket see [balti.migrate.extraBackupsActivity.apps.containers.AppPacket].
 *
 * @param appPacket_z AppPacket for the corresponding app.
 * @param appFileNames String list storing all names of all app related files.
 *                     This included the ".app" directory, ".tar.gz" data file, permissions in ".perm" file, icon file name etc.
 * @param fileSizes File sizes of the above file names. Size of each file is stored in the same index as its name in [appFileNames].
 */
data class ZipAppPacket(
        val appPacket_z: AppPacket,
        val appFileNames: ArrayList<String>,
        val fileSizes: ArrayList<Long>
        ) {

    /**
     * This variable stores the total size of all the files in this packet.
     * It is calculated in [refreshTotal].
     */
    var zipPacketSize: Long = 0
        private set

    /**
     * For non-traditional FileX, this variable stores list of all apk files for an app.
     * Please see [balti.migrate.backupEngines.containers.AppApkFiles].
     * This variable initially stores empty list assuming FileX is traditional.
     * Else an actual list is stored via [addAppApkList].
     */
    var appApkFiles = AppApkFiles(appPacket_z.packageName, listOf())

    init {
        refreshTotal()
    }

    /**
     * Calculate total size of packet.
     * Function is public as it is also called from [balti.migrate.backupEngines.engines.MakeZipBatch.recordAppDirSizeByRoot],
     * after setting the size of ".app" directory.
     */
    fun refreshTotal(){
        zipPacketSize = 0
        fileSizes.forEach {
            zipPacketSize += it
        }
    }

    /**
     * Add APK file names to re-create [appApkFiles]. This method is called for non-traditional FileX.
     * This is called in [balti.migrate.backupEngines.engines.MakeZipBatch.recordAppDirSizeByRoot].
     */
    fun addAppApkList(apkList: List<String>){
        appApkFiles = AppApkFiles(appPacket_z.packageName, apkList)
    }
}