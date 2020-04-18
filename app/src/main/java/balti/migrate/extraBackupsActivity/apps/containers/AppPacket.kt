package balti.migrate.extraBackupsActivity.apps.containers

import android.content.pm.ApplicationInfo
import balti.migrate.backupActivity.containers.BackupDataPacketKotlin
import balti.migrate.utilities.ToolsNoContext

class AppPacket(backupDataPacket: BackupDataPacketKotlin, val appName: String, val dataSizeBytes : Long, val systemSizeBytes : Long) {
    val PACKAGE_INFO = backupDataPacket.PACKAGE_INFO
    val APP = backupDataPacket.APP
    val DATA = backupDataPacket.DATA
    val PERMISSION = backupDataPacket.PERMISSION
    val installerName = backupDataPacket.installerName

    val appInfo = PACKAGE_INFO.applicationInfo
    val packageName = PACKAGE_INFO.packageName
    val apkPath = if (!APP) "NULL" else {
        appInfo.sourceDir.let {
            it.substring(0, it.lastIndexOf('/'))
        }
    }
    val apkName = if (!APP) "NULL" else ToolsNoContext.applyNamingCorrectionForShell(    // this is base apk name, not packageName.apk
        appInfo.sourceDir.let {
            it.substring(it.lastIndexOf('/') + 1)
        }
    )
    val dataPath = if (!DATA) "NULL" else {
        appInfo.dataDir.let {
            it.substring(0, it.lastIndexOf('/'))
        }
    }
    val dataName = if (!DATA) "NULL" else {    // this just points to the data director, does not have tar.gz in the end.
        appInfo.dataDir.let {
            it.substring(it.lastIndexOf('/') + 1)
        }
    }
    //val isSystem = !appInfo.sourceDir.startsWith("/data")
    val isSystem = !(appInfo.flags and (ApplicationInfo.FLAG_SYSTEM or ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) == 0 || appInfo.sourceDir.startsWith("/data"))
}