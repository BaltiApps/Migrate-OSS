package balti.migrate.backupEngines.utils

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import balti.migrate.backupEngines.containers.BackupIntentData
import balti.migrate.extraBackupsActivity.apps.containers.AppPacket
import balti.migrate.utilities.CommonToolKotlin
import org.json.JSONObject
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter

class BackupUtils {

    companion object {
        val ignorableWarnings = arrayOf(
                "socket ignored", "error exit delayed from previous errors"
        )
        val correctableErrors = arrayOf(
                "No such file or directory"
        )
    }

    fun iterateBufferedReader(reader: BufferedReader, loopFunction: (line: String) -> Boolean,
                                      onCancelledFunction: (() -> Unit)? = null){
        var doBreak = false
        while (true){
            val line : String? = reader.readLine()
            if (line == null) break
            else {
                doBreak = loopFunction(line.trim())
                if (doBreak) break
            }
        }
        if (doBreak) onCancelledFunction?.invoke()
    }

    fun makeMetadataFile(isSystem: Boolean, appName: String, apkName: String,
                         dataName: String, iconFileName: String?,
                         version: String, permissions: Boolean,
                         appPacket: AppPacket, bd: BackupIntentData,
                         doBackupInstallers: Boolean, actualDestination: String,
                         iconString: String? = null): String {

        val packageName = appPacket.PACKAGE_INFO.packageName

        val metadataFileName = "$packageName.json"
        val metadataFile = File("${bd.destination}/${bd.backupName}/$metadataFileName")

        val jsonObject = JSONObject()
        jsonObject.apply {
            put(CommonToolKotlin.MTD_IS_SYSTEM, isSystem)
            put(CommonToolKotlin.MTD_APP_NAME, appName)
            put(CommonToolKotlin.MTD_PACKAGE_NAME, packageName)
            put(CommonToolKotlin.MTD_APK, apkName)
            put(CommonToolKotlin.MTD_DATA, dataName)
            put(CommonToolKotlin.MTD_VERSION, version)
            put(CommonToolKotlin.MTD_DATA_SIZE, appPacket.dataSize)
            put(CommonToolKotlin.MTD_SYSTEM_SIZE, appPacket.systemSize)
            put(CommonToolKotlin.MTD_PERMISSION, permissions)
            when {
                iconFileName != null -> put(CommonToolKotlin.MTD_ICON_FILE_NAME, iconFileName)
                iconString != null -> put(CommonToolKotlin.MTD_APP_ICON, iconString)
            }
            put(CommonToolKotlin.MTD_INSTALLER_NAME, if (doBackupInstallers) appPacket.installerName else "NULL")
        }

        File(actualDestination).mkdirs()

        return try {
            val writer = BufferedWriter(FileWriter(metadataFile))
            writer.write(jsonObject.toString(4))
            writer.close()
            ""
        }
        catch (e: Exception){
            e.printStackTrace()
            e.message.toString()
        }
    }

    fun makeIconFile(packageName: String, iconString: String, actualDestination: String): String{

        val iconFileName = "$packageName.icon"
        val iconFile = File("$actualDestination/$iconFileName")

        File(actualDestination).mkdirs()

        try {
            val writer = BufferedWriter(FileWriter(iconFile))
            writer.write(iconString)
            writer.close()
        }
        catch (e: Exception){
            e.printStackTrace()
        }

        return iconFileName
    }

    fun makePermissionFile(packageName: String, actualDestination: String, pm: PackageManager): String{
        val backupPerm = File("$actualDestination/$packageName.perm")
        try {

            val writer = BufferedWriter(FileWriter(backupPerm))

            val pi = pm.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS)
            if (pi.requestedPermissions == null) {
                writer.write("no_permissions_granted")
            } else {
                for (i in pi.requestedPermissions.indices) {
                    if (pi.requestedPermissionsFlags[i] and PackageInfo.REQUESTED_PERMISSION_GRANTED != 0) {
                        val p = pi.requestedPermissions[i].trim { it <= ' ' }
                        if (p.startsWith("android.permission"))
                            writer.write(p + "\n")
                    }
                }
            }

            writer.close()
            return ""
        } catch (e: Exception) {
            e.printStackTrace()
            return e.message.toString()
        }

    }

}