package balti.migrate.backupEngines.utils

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import balti.migrate.backupEngines.BackupIntentData
import balti.migrate.extraBackupsActivity.apps.AppPacket
import balti.migrate.utilities.CommonToolKotlin
import java.io.BufferedWriter
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileWriter

class BackupUtils {

    companion object {
        val ignorableWarnings = arrayOf(
                "socket ignored"
        )
        val correctableErrors = arrayOf(
                "No such file or directory"
        )
    }


    fun getIconString(packageInfo: PackageInfo, pm: PackageManager): String {

        val stream = ByteArrayOutputStream()

        val drawable = pm.getApplicationIcon(packageInfo.applicationInfo)
        val icon = Bitmap.createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(icon)

        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)

        icon.compress(Bitmap.CompressFormat.PNG, 100, stream)

        val bytes = stream.toByteArray()
        var res = ""
        for (b in bytes){
            res = res + b + "_"
        }
        return res
    }

    fun makeMetadataFile(isSystem: Boolean, appName: String, apkName: String,
                                 dataName: String, iconFileName: String?,
                                 version: String, permissions: Boolean,
                                 appPacket: AppPacket, bd: BackupIntentData,
                                 doBackupInstallers: Boolean, actualDestination: String,
                                 iconString: String? = null): String {

        val packageName = appPacket.PACKAGE_INFO.packageName

        val metadataFileName = "$packageName.json"
        val metadataFile = File("${bd.destination}/${bd.destination}/$metadataFileName")

        val metadataContent = "" +
                "{\n" +
                "   \"${CommonToolKotlin.MTD_IS_SYSTEM}\" : \"$isSystem\",\n" +
                "   \"${CommonToolKotlin.MTD_APP_NAME}\" : \"$appName\",\n" +
                "   \"${CommonToolKotlin.MTD_PACKAGE_NAME}\" : \"$packageName\",\n" +
                "   \"${CommonToolKotlin.MTD_APK}\" : \"$apkName\",\n" +
                "   \"${CommonToolKotlin.MTD_DATA}\" : \"$dataName\",\n" +
                "   \"${CommonToolKotlin.MTD_VERSION}\" : \"$version\",\n" +
                "   \"${CommonToolKotlin.MTD_DATA_SIZE}\" : ${appPacket.dataSize},\n" +
                "   \"${CommonToolKotlin.MTD_SYSTEM_SIZE}\" : ${appPacket.systemSize},\n" +
                "   \"${CommonToolKotlin.MTD_PERMISSION}\" : $permissions,\n" +
                when {
                    iconFileName != null -> "   \"${CommonToolKotlin.MTD_ICON_FILE_NAME}\" : \"$iconFileName\"\n"
                    iconString != null -> "   \"${CommonToolKotlin.MTD_APP_ICON}\" : $iconString\n"
                    else -> ""
                } +
                "   \"${CommonToolKotlin.MTD_INSTALLER_NAME}\" : ${
                if (doBackupInstallers) appPacket.installerName
                else "NULL"
                },\n" +
                "}\n"

        File(actualDestination).mkdirs()

        return try {
            val writer = BufferedWriter(FileWriter(metadataFile))
            writer.write(metadataContent)
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

        return try {
            val writer = BufferedWriter(FileWriter(iconFile))
            writer.write(iconString)
            writer.close()
            ""
        }
        catch (e: Exception){
            e.printStackTrace()
            e.message.toString()
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