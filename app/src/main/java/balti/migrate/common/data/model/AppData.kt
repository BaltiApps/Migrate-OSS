package balti.migrate.common.data.model

import android.graphics.drawable.Drawable
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

    override val logInfo: String,

    ): DataItem<AppListItem> {
    override val _id: String = packageName

    val apkPathBase = apkPath.substringBeforeLast('/')

    override fun toListItem(): AppListItem {
        return AppListItem(
            _id = packageName,
            appName = appName,
            appIcon = AppIcon(drawable = appIcon),

            versionName = versionName,
            versionCode = versionCode,

            isApkSelected = shouldBackupApk,
            isDataSelected = shouldBackupData,
            isPermissionsSelected = shouldBackupPermissions,
        )
    }
}