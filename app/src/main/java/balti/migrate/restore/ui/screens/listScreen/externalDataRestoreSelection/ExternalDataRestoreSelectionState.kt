package balti.migrate.restore.ui.screens.listScreen.externalDataRestoreSelection

import baltiapps.migrate.domain.common.model.AppListItem

data class ExternalDataRestoreSelectionState(
    val appListItems: List<AppListItem> = emptyList(),
    val areAllExternalDataSelected: Boolean = false,
    val areAllExternalMediaSelected: Boolean = false,
    val isStaging: Boolean = false,
    val shouldEnableExternalDataSelection: Boolean = true,
    val shouldEnableExternalMediaSelection: Boolean = true,
)
