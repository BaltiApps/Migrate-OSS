package balti.migrate.backup.data.sources.apps

import balti.migrate.common.data.model.AppData
import baltiapps.migrate.domain.AppInfoConstants
import baltiapps.migrate.domain.common.sources.fileSystem.TextWriter
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class AppInfoWriter: TextWriter<AppData> {
    private lateinit var file: File

    override fun setup(fileLocation: String, fileName: String, append: Boolean) {
        File(fileLocation, fileName).run {
            if (exists()) { delete() }
            file = this
        }
    }

    override fun write(data: AppData) {

        val json = JSONObject().apply {
            put(AppInfoConstants.KEY_PACKAGE_NAME, data.packageName)
            put(AppInfoConstants.KEY_APP_NAME, data.appName)
            put(AppInfoConstants.KEY_VERSION_NAME, data.versionName)
            put(AppInfoConstants.KEY_VERSION_CODE, data.versionCode)
            put(AppInfoConstants.KEY_GRANTED_PERMISSIONS, JSONArray(data.grantedPermissionList))
            put(AppInfoConstants.KEY_IS_SYSTEM_APP, data.isSystemApp)
            put(AppInfoConstants.KEY_APK, data.shouldBackupApk)
            put(AppInfoConstants.KEY_DATA, data.shouldBackupData)
            put(AppInfoConstants.KEY_APK_SIZE_BYTES, data.apkSizeBytes)
            put(AppInfoConstants.KEY_DATA_SIZE_BYTES, data.dataSizeBytes)
            put(AppInfoConstants.KEY_PERMISSIONS, data.shouldBackupPermissions)
            put(AppInfoConstants.KEY_INSTALLER, data.installerName)
            put(AppInfoConstants.KEY_EXTERNAL_DATA, data.shouldBackupExternalData)
            put(AppInfoConstants.KEY_EXTERNAL_MEDIA, data.shouldBackupExternalMedia)
        }

        file.writeText(json.toString(4))
    }

    override fun writeLine(data: AppData) {
        write(data)
    }

    override fun close() {}
}