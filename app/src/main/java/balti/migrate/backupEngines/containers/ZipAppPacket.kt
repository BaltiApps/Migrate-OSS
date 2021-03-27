package balti.migrate.backupEngines.containers

import balti.filex.FileX
import balti.migrate.extraBackupsActivity.apps.containers.AppPacket

data class ZipAppPacket(val appPacket_z: AppPacket, val appFiles: ArrayList<FileX>) {
    var zipPacketSize: Long = 0
    private set

    init {
        appFiles.forEach {
            zipPacketSize += it.getDirLength()
        }
    }
}