package balti.migrate.backup.ui.screens.listScreen.appBackupSelection

import baltiapps.migrate.domain.common.model.AppListItem
import baltiapps.migrate.domain.common.model.Progress

data class AppFilterSelection(
    val systemCore: Boolean,
    val systemUpdate: Boolean,
    val userApps: Boolean,
)

data class AppBackupSelectionState(
    val shouldSkipBackup: Boolean = true,
    val progress: Progress = Progress.Empty,
    val appListItems: List<AppListItem> = emptyList(),
    val isStaging: Boolean = false,
    val shouldAskForSuperuserPermission: Boolean = false,
    val areAllApksSelected: Boolean = false,
    val areAllDataSelected: Boolean = false,
    val areAllPermissionsSelected: Boolean = false,
    val filterSelection: AppFilterSelection = AppFilterSelection(
        systemCore = false,
        systemUpdate = false,
        userApps = true,
    ),
    val isAllPermissionsCheckboxDisabled: Boolean = false,
    val searchText: String = "",
    val showSearchBar: Boolean = false,
    val displayedAppListItems: List<AppListItem> = emptyList(),
)