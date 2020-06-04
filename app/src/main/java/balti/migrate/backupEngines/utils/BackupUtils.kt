package balti.migrate.backupEngines.utils

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import balti.migrate.backupEngines.containers.BackupIntentData
import balti.migrate.extraBackupsActivity.apps.containers.AppPacket
import balti.migrate.utilities.CommonToolKotlin.Companion.KB_DIVISION_SIZE
import balti.migrate.utilities.CommonToolKotlin.Companion.PACKAGE_NAMES_KNOWN
import balti.migrate.utilities.constants.MtdConstants
import org.json.JSONObject
import java.io.*

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

    fun makeMetadataFile(version: String,
                         iconFileName: String?, iconString: String?,
                         appPacket: AppPacket, bd: BackupIntentData, doBackupInstallerName: Boolean): String{

        val packageName = appPacket.PACKAGE_INFO.packageName
        val metadataFileName = "$packageName.json"
        val actualDestination = File("${bd.destination}/${bd.backupName}")
        val metadataFile = File(actualDestination, metadataFileName)

        val jsonObject = JSONObject()
        jsonObject.apply {
            put(MtdConstants.MTD_IS_SYSTEM, appPacket.isSystem)
            put(MtdConstants.MTD_APP_NAME, appPacket.appName)
            put(MtdConstants.MTD_PACKAGE_NAME, packageName)
            put(MtdConstants.MTD_APK, if (appPacket.APP) "$packageName.apk" else "NULL")
            put(MtdConstants.MTD_DATA, if (appPacket.DATA) "$packageName.tar.gz" else "NULL")
            put(MtdConstants.MTD_VERSION, version)
            put(MtdConstants.MTD_DATA_SIZE, appPacket.dataSizeBytes / KB_DIVISION_SIZE)
            put(MtdConstants.MTD_SYSTEM_SIZE, appPacket.systemSizeBytes / KB_DIVISION_SIZE)
            put(MtdConstants.MTD_PERMISSION, appPacket.PERMISSION)
            when {
                iconFileName != null -> put(MtdConstants.MTD_ICON_FILE_NAME, iconFileName)
                iconString != null -> put(MtdConstants.MTD_APP_ICON, iconString)
            }
            put(MtdConstants.MTD_INSTALLER_NAME,
                    if (doBackupInstallerName)
                        appPacket.installerName.let {
                            if (it in PACKAGE_NAMES_KNOWN) it else "NULL"
                        }
                    else "NULL"
            )
        }

        actualDestination.mkdirs()

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

    /*fun makeMetadataFile(isSystem: Boolean, appName: String, apkName: String,
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
            put(MtdConstants.MTD_IS_SYSTEM, isSystem)
            put(MtdConstants.MTD_APP_NAME, appName)
            put(MtdConstants.MTD_PACKAGE_NAME, packageName)
            put(MtdConstants.MTD_APK, apkName)
            put(MtdConstants.MTD_DATA, dataName)
            put(MtdConstants.MTD_VERSION, version)
            put(MtdConstants.MTD_DATA_SIZE, appPacket.dataSize)
            put(MtdConstants.MTD_SYSTEM_SIZE, appPacket.systemSize)
            put(MtdConstants.MTD_PERMISSION, permissions)
            when {
                iconFileName != null -> put(MtdConstants.MTD_ICON_FILE_NAME, iconFileName)
                iconString != null -> put(MtdConstants.MTD_APP_ICON, iconString)
            }
            put(MtdConstants.MTD_INSTALLER_NAME, if (doBackupInstallers) appPacket.installerName else "NULL")
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
    }*/

    fun makeStringIconFile(packageName: String, iconString: String, actualDestination: String): String{

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

    fun makeNewIconFile(packageName: String, icon: Bitmap, actualDestination: String): String{

        val iconFileName = "$packageName.png"
        val iconFile = File("$actualDestination/$iconFileName")

        File(actualDestination).mkdirs()

        try {
            val fos = FileOutputStream(iconFile)
            icon.compress(Bitmap.CompressFormat.PNG, 80, fos)
            fos.close()
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
            val perms = pi.requestedPermissions
            val granted = ArrayList<String>(0)

            if (perms != null && perms.isNotEmpty()) {
                for (i in perms.indices) {
                    if (pi.requestedPermissionsFlags[i] and PackageInfo.REQUESTED_PERMISSION_GRANTED != 0) {
                        val p = perms[i].trim()
                        if (p.startsWith("android.permission")) granted.add(p)
                    }
                }
            }

            if (granted.isEmpty()) writer.write("no_permissions_granted\n")
            else granted.forEach {
                writer.write("$it\n")
            }

            writer.close()
            return ""
        } catch (e: Exception) {
            e.printStackTrace()
            return e.message.toString()
        }

    }

}