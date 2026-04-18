package balti.migrate.backup.ui.screens.listScreen.extraOptions.externalData

import baltiapps.migrate.domain.common.model.AppListItem

data class ExternalDataBackupSelectionState(
    val appListItems: List<AppListItem> = emptyList(),
    val areAllExternalDataSelected: Boolean = false,
    val areAllExternalMediaSelected: Boolean = false,
    val isStaging: Boolean = false,
)
