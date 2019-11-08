package balti.migrate.backupEngines.containers

import balti.migrate.extraBackupsActivity.apps.containers.AppPacket
import balti.migrate.utilities.ToolsNoContext
import java.io.File

data class ZipAppPacket(val appPacket_z: AppPacket, val appFiles: ArrayList<File>) {
    var zipPacketSize: Long = 0
    private set

    init {
        appFiles.forEach {
            zipPacketSize += ToolsNoContext.getDirLength(it)
        }
        zipPacketSize /= 1024
    }
}