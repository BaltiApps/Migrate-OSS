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

    val isVersionLowerThanInstalled: Boolean = false,
    val isSelf: Boolean = false,

    val isSystemApp: Boolean,
    val isUpdatedSystemApp: Boolean,

): ListItem {
    fun isAnySelected(): Boolean {
        val variablesToConsider = mutableListOf<Boolean>()

        if (isApkEnabled) variablesToConsider.add(isApkSelected)
        if (isDataEnabled) variablesToConsider.add(isDataSelected)
        if (isPermissionsEnabled) variablesToConsider.add(isPermissionsSelected)

        return variablesToConsider.any { it }
    }
    fun isAllSelected(): Boolean {
        val variablesToConsider = mutableListOf<Boolean>()

        if (isApkEnabled) variablesToConsider.add(isApkSelected)
        if (isDataEnabled) variablesToConsider.add(isDataSelected)
        if (isPermissionsEnabled) variablesToConsider.add(isPermissionsSelected)

        return variablesToConsider.isNotEmpty() && variablesToConsider.all { it }
    }
}
