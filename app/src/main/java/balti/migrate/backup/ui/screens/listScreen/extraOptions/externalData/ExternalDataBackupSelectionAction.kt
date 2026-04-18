package balti.migrate.backup.ui.screens.listScreen.extraOptions.externalData

import baltiapps.migrate.domain.common.model.AppListItem

sealed class ExternalDataBackupSelectionAction {
    data class ToggleExternalData(val item: AppListItem) : ExternalDataBackupSelectionAction()
    data class ToggleExternalMedia(val item: AppListItem) : ExternalDataBackupSelectionAction()
    data class ToggleItemBoth(val item: AppListItem) : ExternalDataBackupSelectionAction()
    data class ToggleAllExternalData(val isChecked: Boolean) : ExternalDataBackupSelectionAction()
    data class ToggleAllExternalMedia(val isChecked: Boolean) : ExternalDataBackupSelectionAction()
    data class ToggleAllBoth(val isChecked: Boolean) : ExternalDataBackupSelectionAction()
    data class Save(val onSaved: () -> Unit) : ExternalDataBackupSelectionAction()
}
