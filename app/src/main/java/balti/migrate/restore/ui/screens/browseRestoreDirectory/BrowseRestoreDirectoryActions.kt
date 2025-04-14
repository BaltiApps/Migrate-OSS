package balti.migrate.restore.ui.screens.browseRestoreDirectory

import android.app.Activity
import baltiapps.migrate.domain.common.model.Directory

sealed class BrowseRestoreDirectoryActions {
    data class RequestPermission(val activity: Activity?): BrowseRestoreDirectoryActions()
    data object OnReloadDirectoryContents: BrowseRestoreDirectoryActions()
    data class OnDirectoryOpen(val directory: Directory): BrowseRestoreDirectoryActions()
    data object OnDirectoryUp: BrowseRestoreDirectoryActions()
    data class OnBackupSelected(
        val directory: Directory,
        val onLoadingFinished: () -> Unit,
    ): BrowseRestoreDirectoryActions()
}