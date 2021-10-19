package balti.migrate.backupEngines.utils

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import balti.filex.FileX
import balti.migrate.AppInstance
import balti.migrate.extraBackupsActivity.apps.containers.AppPacket
import balti.migrate.utilities.CommonToolsKotlin.Companion.KB_DIVISION_SIZE
import balti.migrate.utilities.constants.MtdConstants
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter

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

    fun makeMetadataFile(traditionalLocation: String, version: String,
                         iconFileName: String, appPacket: AppPacket): String{

        val packageName = appPacket.PACKAGE_INFO.packageName
        val metadataFileName = "$packageName.json"
        val metadataFile = FileX.new("${traditionalLocation}/${metadataFileName}", true)
        metadataFile.createNewFile(makeDirectories = true, overwriteIfExists = true)

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
            put(MtdConstants.MTD_SPLITS, JSONArray(appPacket.PACKAGE_INFO.splitNames?: arrayOf<String>()))
            put(MtdConstants.MTD_ICON_FILE_NAME, iconFileName)
            put(MtdConstants.MTD_INSTALLER_NAME,
                    AppInstance.appInstallers[packageName].let {
                        if (it?.isNotBlank() == true) it
                        else "NULL"
                    }
            )
        }

        return try {
            metadataFile.writeOneLine(jsonObject.toString(4))
            ""
        }
        catch (e: Exception){
            e.printStackTrace()
            e.message.toString()
        }
    }

    fun makeStringIconFile(packageName: String, iconString: String, traditionalLocation: String): String{

        val iconFileName = "$packageName.icon"
        val iconFile = FileX.new("$traditionalLocation/$iconFileName", true)
        iconFile.createNewFile(makeDirectories = true, overwriteIfExists = true)

        try {
            iconFile.writeOneLine(iconString)
        }
        catch (e: Exception){
            e.printStackTrace()
        }

        return iconFileName
    }

    fun makeNewIconFile(packageName: String, icon: Bitmap, traditionalLocation: String): String{

        val iconFileName = "$packageName.png"
        val iconFile = FileX.new("$traditionalLocation/$iconFileName", true)
        iconFile.createNewFile(makeDirectories = true, overwriteIfExists = true)

        try {
            val fos = iconFile.outputStream()
            icon.compress(Bitmap.CompressFormat.PNG, 80, fos)
            fos?.close()
        }
        catch (e: Exception){
            e.printStackTrace()
        }

        return iconFileName
    }

    fun makePermissionFile(packageName: String, traditionalLocation: String, pm: PackageManager): String{
        val permFile = FileX.new("$traditionalLocation/$packageName.perm", true)
        permFile.createNewFile(overwriteIfExists = true, makeDirectories = true)
        try {

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

            permFile.startWriting(object : FileX.Writer(){
                override fun writeLines() {
                    if (granted.isEmpty()) writeLine("no_permissions_granted")
                    else granted.forEach {
                        writeLine(it)
                    }
                }
            })

            return ""
        } catch (e: Exception) {
            e.printStackTrace()
            return e.message.toString()
        }

    }

    fun moveAuxFilesToBackupLocation(auxDirectory: String, backupLocation: String): ArrayList<String> {
        val errors = ArrayList<String>(0)

        val suProcess = Runtime.getRuntime().exec("su")
        suProcess?.let {
            val suInputStream = BufferedWriter(OutputStreamWriter(it.outputStream))
            val errorStream = BufferedReader(InputStreamReader(it.errorStream))

            suInputStream.write("cp $auxDirectory/* $backupLocation/ && rm -rf $auxDirectory/*\n")
            suInputStream.write("exit\n")
            suInputStream.flush()

            iterateBufferedReader(errorStream, { errorLine ->
                errors.add(errorLine)
                return@iterateBufferedReader false
            })
        }

        return errors
    }

}