package balti.migrate.restore.ui.screens.listScreen.appRestoreSelection

import baltiapps.migrate.domain.common.model.AppListItem

sealed class AppRestoreSelectionAction {
    object AskSuPermission: AppRestoreSelectionAction()

    data class ToggleAppItemApkSelection(val item: AppListItem): AppRestoreSelectionAction()
    data class ToggleAppItemDataSelection(val item: AppListItem): AppRestoreSelectionAction()
    data class ToggleAppItemPermissionSelection(val item: AppListItem): AppRestoreSelectionAction()

    data class ToggleEverythingForAnApp(val item: AppListItem): AppRestoreSelectionAction()

    data class ToggleAllAppItemsApkSelection(val isChecked: Boolean): AppRestoreSelectionAction()
    data class ToggleAllAppItemsDataSelection(val isChecked: Boolean): AppRestoreSelectionAction()
    data class ToggleAllAppItemsPermissionSelection(val isChecked: Boolean): AppRestoreSelectionAction()

    data class ToggleAllAppItems(val isChecked: Boolean): AppRestoreSelectionAction()

    data class StageAppItems(val onStagingDone: () -> Unit): AppRestoreSelectionAction()
}