package balti.migrate.common.data.model

import android.graphics.drawable.Drawable
import balti.migrate.BuildConfig
import baltiapps.migrate.domain.common.model.AppListItem
import baltiapps.migrate.domain.common.model.DataItem

data class AppData(
    val packageName: String,
    val appName: String,
    val appIcon: Drawable,

    val versionName: String,
    val versionCode: Long,

    val apkPath: String,
    val dataPath: String,

    val grantedPermissionList: List<String>,

    val uid: Int,
    val gid: Int,

    val isSystemApp: Boolean,
    val isUpdatedSystemApp: Boolean,

    val shouldBackupApk: Boolean = true,
    val shouldBackupData: Boolean = true,
    val shouldBackupPermissions: Boolean = true,

    val shouldBackupExternalData: Boolean = false,
    val shouldBackupExternalMedia: Boolean = false,

    val installerName: String,

    val user: Int,

    val apkSizeBytes: Long = 0,
    val dataSizeBytes: Long = 0,
    val externalDataBytes: Long = 0,
    val externalMediaBytes: Long = 0,

    override val logInfo: String,

    ): DataItem<AppListItem> {
    override val _id: String = packageName
    val isSelf: Boolean = packageName == BuildConfig.APPLICATION_ID

    val apkPathBase = apkPath.substringBeforeLast('/')

    override fun toListItem(): AppListItem {
        return AppListItem(
            _id = packageName,
            appName = appName,
            appIcon = AppIcon(drawable = appIcon),

            versionName = versionName,
            versionCode = versionCode,

            isApkSelected = shouldBackupApk && !isSelf,
            isDataSelected = shouldBackupData && !isSelf,
            isPermissionsSelected = shouldBackupPermissions && !isSelf,

            isApkEnabled = apkSizeBytes != 0L && !isSelf,
            isDataEnabled = dataSizeBytes != 0L && !isSelf,
            isPermissionsEnabled = grantedPermissionList.isNotEmpty() && !isSelf,

            isExternalDataSelected = shouldBackupExternalData && !isSelf,
            isExternalMediaSelected = shouldBackupExternalMedia && !isSelf,

            isExternalDataEnabled = externalDataBytes != 0L && !isSelf,
            isExternalMediaEnabled = externalMediaBytes != 0L && !isSelf,

            isVersionLowerThanInstalled = false,
            isSelf = isSelf,

            isSystemApp = isSystemApp,
            isUpdatedSystemApp = isUpdatedSystemApp,
        )
    }
}