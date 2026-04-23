package balti.migrate.backup.ui.screens.listScreen.appBackupSelection

import baltiapps.migrate.domain.common.model.AppListItem

sealed class AppBackupSelectionAction {
    object AskSuPermission: AppBackupSelectionAction()

    data class ToggleAppItemApkSelection(val item: AppListItem): AppBackupSelectionAction()
    data class ToggleAppItemDataSelection(val item: AppListItem): AppBackupSelectionAction()
    data class ToggleAppItemPermissionSelection(val item: AppListItem): AppBackupSelectionAction()

    data class ToggleEverythingForAnApp(val item: AppListItem): AppBackupSelectionAction()

    data class ToggleAllAppItemsApkSelection(val isChecked: Boolean): AppBackupSelectionAction()
    data class ToggleAllAppItemsDataSelection(val isChecked: Boolean): AppBackupSelectionAction()
    data class ToggleAllAppItemsPermissionSelection(val isChecked: Boolean): AppBackupSelectionAction()

    data class ToggleAllAppItems(val isChecked: Boolean): AppBackupSelectionAction()

    data class StageAppItems(val onStagingDone: () -> Unit): AppBackupSelectionAction()

    data class UpdateFilterSelection(val filterSelection: AppFilterSelection): AppBackupSelectionAction()

    data class UpdateSearchText(val searchText: String): AppBackupSelectionAction()
    object ToggleSearchBar: AppBackupSelectionAction()
}