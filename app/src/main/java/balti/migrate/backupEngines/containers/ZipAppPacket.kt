package balti.migrate.backupEngines.containers

import balti.filex.FileX
import balti.migrate.extraBackupsActivity.apps.containers.AppPacket

data class ZipAppPacket(
        val appPacket_z: AppPacket,
        val appFileNames: ArrayList<String>,
        val fileSizes: ArrayList<Long>
        ) {
    var zipPacketSize: Long = 0
    private set

    init {
        refreshTotal()
    }

    fun refreshTotal(){
        fileSizes.forEach {
            zipPacketSize += it
        }
    }
}