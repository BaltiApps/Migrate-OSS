package balti.migrate.backupEngines.containers

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
 *                     Does NOT include base/split apks individually.
 * @param fileSizes File sizes of the above file names.
 *                  Size of each file is stored in the same index as its name in [appFileNames].
 *                  Size of ".app" dir to be passed as 0.
 * @param appApkFiles List of Pair of apk name (base apk and split apks) and size.
 */
data class ZipAppPacket(
        val appPacket_z: AppPacket,
        val appFileNames: ArrayList<String>,
        val fileSizes: ArrayList<Long>,
        val appApkFiles: ArrayList<Pair<String, Long>>
        ): ParentZipPacket() {

    /**
     * This variable stores the total size of all the files in this packet.
     * It is calculated in [refreshTotal].
     */
    override var zipPacketSize: Long = 0

    /**
     * A string for logging purposes.
     */
    override val displayName: String = appPacket_z.appName

    init {
        refreshTotal()
    }

    /**
     * Calculate total size of packet.
     */
    override fun refreshTotal(){
        zipPacketSize = 0
        fileSizes.forEach {
            zipPacketSize += it
        }
        appApkFiles.forEach {
            zipPacketSize += it.second
        }
    }
}