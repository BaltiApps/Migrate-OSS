package balti.migrate.restore.ui.screens.listScreen.externalDataRestoreSelection

import baltiapps.migrate.domain.common.model.AppListItem

sealed class ExternalDataRestoreSelectionAction {
    data class ToggleExternalData(val item: AppListItem) : ExternalDataRestoreSelectionAction()
    data class ToggleExternalMedia(val item: AppListItem) : ExternalDataRestoreSelectionAction()
    data class ToggleItemBoth(val item: AppListItem) : ExternalDataRestoreSelectionAction()
    data class ToggleAllExternalData(val isChecked: Boolean) : ExternalDataRestoreSelectionAction()
    data class ToggleAllExternalMedia(val isChecked: Boolean) : ExternalDataRestoreSelectionAction()
    data class ToggleAllBoth(val isChecked: Boolean) : ExternalDataRestoreSelectionAction()
    data class Save(val onSaved: () -> Unit) : ExternalDataRestoreSelectionAction()
}
