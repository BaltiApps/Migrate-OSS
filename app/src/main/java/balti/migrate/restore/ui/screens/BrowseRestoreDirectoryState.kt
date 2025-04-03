package balti.migrate.restore.ui.screens

import baltiapps.migrate.domain.common.model.Directory

data class BrowseRestoreDirectoryState(
    val isLoading: Boolean,
    val directoriesToShow: List<Directory>,
    val currentDirectory: Directory,
) {
    val isBackAllowed: Boolean
        get() = currentDirectory.parent != null
}
