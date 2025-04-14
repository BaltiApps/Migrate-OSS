package balti.migrate.restore.ui.screens.browseRestoreDirectory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import balti.migrate.MainActivity
import baltiapps.migrate.domain.DEFAULT_BACKUP_ROOT
import baltiapps.migrate.domain.PermissionConstants
import baltiapps.migrate.domain.common.model.Directory
import baltiapps.migrate.domain.common.sources.ContextSource
import baltiapps.migrate.domain.common.sources.fileSystem.DirectoryBrowser
import baltiapps.migrate.domain.restore.usecase.ReadFilesFromBackupUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class BrowseRestoreDirectoryViewModel(
    private val directoryBrowser: DirectoryBrowser,
    private val readFilesFromBackupUseCase: ReadFilesFromBackupUseCase,
    private val contextSource: ContextSource,
): ViewModel() {

    private val _state = MutableStateFlow(
        BrowseRestoreDirectoryState(
            hasPermission = false,
            isLoading = false,
            directoriesToShow = listOf(),
            currentDirectory = Directory(
                directoryFullPath = DEFAULT_BACKUP_ROOT,
                basePath = DEFAULT_BACKUP_ROOT,
                name = "",
                parent = null,
                creationTime = 0,
                isValidBackupDirectory = false,
            )
        )
    )
    val state = _state.asStateFlow()

    private val permission = PermissionConstants.MANAGE_EXTERNAL_STORAGE

    init {
        onAction(BrowseRestoreDirectoryActions.OnReloadDirectoryContents)
    }

    private fun loadDirectory(directory: Directory) {
        viewModelScope.launch(Dispatchers.IO) {
            if (!contextSource.checkPermission(permission)) {
                _state.update { it.copy(hasPermission = false) }
                return@launch
            }
            _state.update {
                it.copy(
                    hasPermission = true,
                    isLoading = true,
                )
            }
            val contents = directoryBrowser.getDirectoriesUnder(directory).run {
                this.sortedByDescending { it.creationTime }
            }
            _state.update {
                it.copy(
                    hasPermission = true,
                    isLoading = false,
                    directoriesToShow = contents,
                    currentDirectory = directory,
                )
            }
        }
    }

    fun onAction(action: BrowseRestoreDirectoryActions) {
        if (_state.value.isLoading) return
        viewModelScope.launch {
            when(action) {
                is BrowseRestoreDirectoryActions.RequestPermission -> {
                    if (action.activity !is MainActivity) return@launch
                    action.activity.requestPermission(permission) {
                        loadDirectory(_state.value.currentDirectory)
                    }
                }
                is BrowseRestoreDirectoryActions.OnReloadDirectoryContents -> {
                    loadDirectory(_state.value.currentDirectory)
                }
                is BrowseRestoreDirectoryActions.OnDirectoryOpen -> {
                    loadDirectory(action.directory)
                }
                is BrowseRestoreDirectoryActions.OnDirectoryUp -> {
                    _state.value.currentDirectory.parent?.let { loadDirectory(it) }
                }
                is BrowseRestoreDirectoryActions.OnBackupSelected -> {
                    _state.update { it.copy(isLoading = true) }
                    readFilesFromBackupUseCase.invoke(action.directory)
                    action.onLoadingFinished()
                    _state.update { it.copy(isLoading = false) }
                }
            }
        }
    }
}