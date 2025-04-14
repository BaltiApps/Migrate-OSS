package balti.migrate.restore.ui.screens.browseRestoreDirectory

import baltiapps.migrate.domain.common.model.Directory

data class BrowseRestoreDirectoryState(
    val hasPermission: Boolean,
    val isLoading: Boolean,
    val directoriesToShow: List<Directory>,
    val currentDirectory: Directory,
) {
    val isBackAllowed: Boolean
        get() = currentDirectory.parent != null
}
