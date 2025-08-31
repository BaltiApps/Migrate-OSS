package balti.migrate.backup.ui.screens.listScreen.appBackupSelection

import baltiapps.migrate.domain.common.model.AppListItem
import baltiapps.migrate.domain.common.model.Progress

data class AppBackupSelectionState(
    val shouldSkipBackup: Boolean = true,
    val progress: Progress = Progress.Empty,
    val appListItems: List<AppListItem> = emptyList(),
    val isStaging: Boolean = false,
    val shouldAskForSuperuserPermission: Boolean = false,
    val areAllApksSelected: Boolean = false,
    val areAllDataSelected: Boolean = false,
    val areAllPermissionsSelected: Boolean = false,
)