package balti.migrate.ng.backupEngines.containers

import balti.migrate.ng.extraBackupsActivity.apps.containers.AppPacket
import balti.migrate.ng.utilities.ToolsNoContext
import java.io.File

data class ZipAppPacket(val appPacket_z: AppPacket, val appFiles: ArrayList<File>) {
    var zipPacketSize: Long = 0
    private set

    init {
        appFiles.forEach {
            zipPacketSize += ToolsNoContext.getDirLength(it)
        }
    }
}