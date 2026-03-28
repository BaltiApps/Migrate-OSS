package balti.migrate.restore.data.sources.apps

import android.graphics.Color
import androidx.core.graphics.drawable.toDrawable
import balti.migrate.common.data.model.AppData
import baltiapps.migrate.domain.AppInfoConstants
import baltiapps.migrate.domain.common.model.Progress
import baltiapps.migrate.domain.common.sources.fileSystem.TextReader
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import org.json.JSONObject
import java.io.File

class AppInfoReader: TextReader<AppData> {
    private lateinit var file: File

    override fun setup(
        fileLocation: String,
        fileName: String,
    ) {
        file = File(fileLocation, fileName)
    }

    override fun read(): AppData {
        val json = JSONObject(file.readText())

        val permsJsonArray = json.optJSONArray(AppInfoConstants.KEY_GRANTED_PERMISSIONS)
        val grantedPermissions: List<String> = permsJsonArray?.run {
            List(permsJsonArray.length()) { i ->
                permsJsonArray.getString(i)
            }
        } ?: listOf()

        val packageName = json.optString(AppInfoConstants.KEY_PACKAGE_NAME)
        val appName = json.optString(AppInfoConstants.KEY_APP_NAME)

        val appData = AppData(
            packageName = packageName,
            appName = appName,

            appIcon = Color.TRANSPARENT.toDrawable(),

            versionName = json.optString(AppInfoConstants.KEY_VERSION_NAME),
            versionCode = json.optLong(AppInfoConstants.KEY_VERSION_CODE),

            apkPath = "",
            dataPath = "",

            grantedPermissionList = grantedPermissions,

            uid = -1,
            gid = -1,

            isSystemApp = json.optBoolean(AppInfoConstants.KEY_IS_SYSTEM_APP),
            isUpdatedSystemApp = false,

            shouldBackupApk = json.optBoolean(AppInfoConstants.KEY_APK),
            shouldBackupData = json.optBoolean(AppInfoConstants.KEY_DATA),
            shouldBackupPermissions = json.optBoolean(AppInfoConstants.KEY_PERMISSIONS),
            
            installerName = json.optString(AppInfoConstants.KEY_INSTALLER),

            user = 0, // TODO: find way to pass the actual user

            logInfo = "$appName : ($packageName)"
        )
        return appData
    }

    override fun readLines(onFinished: (List<AppData>) -> Unit): Flow<Progress> {
        onFinished(listOf(read()))
        return emptyFlow()
    }

    override fun close() {}
}