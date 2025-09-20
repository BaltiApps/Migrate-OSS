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

): ListItem {
    fun isAnySelected() = isApkSelected || isDataSelected || isPermissionsSelected
    fun isAllSelected() = isApkSelected && isDataSelected && isPermissionsSelected
}
