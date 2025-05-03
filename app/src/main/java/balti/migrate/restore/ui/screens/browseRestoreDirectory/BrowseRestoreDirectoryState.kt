package balti.migrate.restore.ui.screens.browseRestoreDirectory

import baltiapps.migrate.domain.common.model.GenericFile

data class BrowseRestoreDirectoryState(
    val isLoading: Boolean,
    val exportDirectoriesToShow: List<GenericFile>,
    val currentExportDirectory: GenericFile,
    val isImporting: Boolean,
    val isBackAllowed: Boolean,
) {
}
