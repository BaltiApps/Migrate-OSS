package balti.migrate.restore.ui.screens.listScreen.appRestoreSelection

import baltiapps.migrate.domain.common.model.AppListItem
import baltiapps.migrate.domain.common.model.Progress

data class AppRestoreSelectionState(
    val shouldSkipRestore: Boolean = true,
    val progress: Progress = Progress.Empty,
    val appListItems: List<AppListItem> = emptyList(),
    val isStaging: Boolean = false,
    val shouldAskForSuperuserPermission: Boolean = false,
    val areAllApksSelected: Boolean = false,
    val areAllDataSelected: Boolean = false,
    val areAllPermissionsSelected: Boolean = false,
    val shouldEnableApkSelection: Boolean = true,
    val shouldEnableDataSelection: Boolean = true,
    val shouldEnablePermissionSelection: Boolean = true,
)