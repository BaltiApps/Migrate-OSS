package balti.migrate.restore.ui.screens.browseRestoreDirectory

import baltiapps.migrate.domain.common.model.GenericFile

sealed class BrowseRestoreDirectoryActions {
    data object OnReloadExportDirectory: BrowseRestoreDirectoryActions()
    data class OnExportDirectoryOpen(val directory: GenericFile): BrowseRestoreDirectoryActions()
    data object OnExportDirectoryUp: BrowseRestoreDirectoryActions()
    data class OnExportDirectorySelected(
        val directory: GenericFile,
        val onImportFinished: () -> Unit,
    ): BrowseRestoreDirectoryActions()
}