package balti.migrate.backup.ui.screens.listScreen.extraOptions.externalData

import baltiapps.migrate.domain.common.model.AppListItem

sealed class ExternalDataAction {
    data class ToggleExternalData(val item: AppListItem) : ExternalDataAction()
    data class ToggleExternalMedia(val item: AppListItem) : ExternalDataAction()
    data class ToggleItemBoth(val item: AppListItem) : ExternalDataAction()
    data class ToggleAllExternalData(val isChecked: Boolean) : ExternalDataAction()
    data class ToggleAllExternalMedia(val isChecked: Boolean) : ExternalDataAction()
    data class ToggleAllBoth(val isChecked: Boolean) : ExternalDataAction()
    data class Save(val onSaved: () -> Unit) : ExternalDataAction()
}
