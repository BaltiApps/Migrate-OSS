package balti.migrate.restore.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import baltiapps.migrate.domain.DEFAULT_BACKUP_ROOT
import baltiapps.migrate.domain.common.model.Directory
import baltiapps.migrate.domain.common.sources.DirectoryBrowser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BrowseRestoreDirectoryViewModel(
    private val directoryBrowser: DirectoryBrowser
): ViewModel() {

    private val _state = MutableStateFlow(
        BrowseRestoreDirectoryState(
            isLoading = true,
            directoriesToShow = listOf(),
            currentDirectory = Directory(
                directoryFullPath = DEFAULT_BACKUP_ROOT,
                basePath = DEFAULT_BACKUP_ROOT,
                name = "",
                parent = null,
                isValidBackupDirectory = false,
            )
        )
    )
    val state = _state.asStateFlow()

    init {
        onAction(BrowseRestoreDirectoryActions.OnReloadDirectoryContents)
    }

    private fun loadDirectory(directory: Directory) {
        viewModelScope.launch(Dispatchers.IO) {
            _state.update { it.copy(isLoading = true) }
            val contents = directoryBrowser.getDirectoriesUnder(directory)
            _state.update {
                it.copy(
                    isLoading = false,
                    directoriesToShow = contents,
                    currentDirectory = directory,
                )
            }
        }
    }

    fun onAction(action: BrowseRestoreDirectoryActions) {
        viewModelScope.launch {
            when(action) {
                is BrowseRestoreDirectoryActions.OnReloadDirectoryContents -> {
                    loadDirectory(_state.value.currentDirectory)
                }
                is BrowseRestoreDirectoryActions.OnDirectoryOpen -> {
                    loadDirectory(action.directory)
                }
                is BrowseRestoreDirectoryActions.OnDirectoryUp -> {
                    _state.value.currentDirectory.parent?.let { loadDirectory(it) }
                }
            }
        }
    }
}