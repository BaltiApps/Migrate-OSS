package balti.migrate.extraBackupsActivity.apps

import balti.migrate.backupActivity.BackupDataPacketKotlin

class AppPacket(backupDataPacket: BackupDataPacketKotlin, val dataSize : Long, val systemSize : Long) {
    val PACKAGE_INFO = backupDataPacket.PACKAGE_INFO
    val APP = backupDataPacket.APP
    val DATA = backupDataPacket.DATA
    val PERMISSION = backupDataPacket.PERMISSION
    val installerName = backupDataPacket.installerName
}