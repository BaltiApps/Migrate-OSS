package baltiapps.migrate.domain.common.model

data class AppListItem(
    override val _id: String,
    val appName: String,
    val appIcon: DrawableAsset,

    val versionName: String,
    val versionCode: Long,

    val isApkSelected: Boolean,
    val isDataSelected: Boolean,
    val isPermissionsSelected: Boolean,

    val isApkEnabled: Boolean = true,
    val isDataEnabled: Boolean = true,
    val isPermissionsEnabled: Boolean = true,

    val isExternalDataSelected: Boolean = false,
    val isExternalMediaSelected: Boolean = false,

    val isExternalDataEnabled: Boolean = true,
    val isExternalMediaEnabled: Boolean = true,

    val isSystemApp: Boolean,
    val isUpdatedSystemApp: Boolean,

): ListItem {
    fun isAnySelected() = isApkSelected || isDataSelected || isPermissionsSelected
    fun isAllSelected() = isApkSelected && isDataSelected && isPermissionsSelected
}
