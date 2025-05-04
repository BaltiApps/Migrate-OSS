package balti.migrate.restore.ui.screens.browseRestoreDirectory

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import balti.migrate.common.data.model.JavaFile
import balti.migrate.common.data.model.MediaStoreDownloadFile
import baltiapps.migrate.domain.INTERNAL_ROUGH_WORK_RESTORE_DIRECTORY
import baltiapps.migrate.domain.common.model.GenericFile
import baltiapps.migrate.domain.restore.sources.ExportDirectoryBrowser
import baltiapps.migrate.domain.restore.usecase.ReadFilesFromBackupUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class BrowseRestoreDirectoryViewModel(
    private val exportDirectoryBrowser: ExportDirectoryBrowser<GenericFile>,
    private val readFilesFromBackupUseCase: ReadFilesFromBackupUseCase,
    private val applicationContext: Context,
): ViewModel() {

    private val basePath = MediaStoreDownloadFile.EXPORT_PATH_PREFIX

    private val _state = MutableStateFlow(
        BrowseRestoreDirectoryState(
            isLoading = false,
            exportDirectoriesToShow = listOf(),
            currentExportDirectory = MediaStoreDownloadFile(
                path = basePath
            ),
            isImporting = false,
            isBackAllowed = false,
        )
    )
    val state = _state.asStateFlow()

    init {
        onAction(BrowseRestoreDirectoryActions.OnReloadExportDirectory)
    }

    private fun loadDirectory(directory: GenericFile) {
        viewModelScope.launch(Dispatchers.IO) {
            _state.update {
                it.copy(isLoading = true)
            }
            val subDirectoryList = exportDirectoryBrowser.getDirectories(directory)
            _state.update {
                it.copy(
                    isLoading = false,
                    exportDirectoriesToShow = subDirectoryList,
                    currentExportDirectory = directory,
                    isBackAllowed = directory.path.startsWith(basePath) && directory.path != basePath,
                )
            }
        }
    }

    fun onAction(action: BrowseRestoreDirectoryActions) {
        if (_state.value.isLoading) return
        viewModelScope.launch {
            when(action) {
                is BrowseRestoreDirectoryActions.OnReloadExportDirectory -> {
                    loadDirectory(_state.value.currentExportDirectory)
                }
                is BrowseRestoreDirectoryActions.OnExportDirectoryOpen -> {
                    loadDirectory(action.directory)
                }
                is BrowseRestoreDirectoryActions.OnExportDirectoryUp -> {
                    val currentDirectoryPath = _state.value.currentExportDirectory.path
                    val parentPath = currentDirectoryPath.substringBeforeLast("/")
                    loadDirectory(
                        MediaStoreDownloadFile(
                            path = parentPath,
                        )
                    )
                }
                is BrowseRestoreDirectoryActions.OnExportDirectorySelected -> {
                    _state.update { it.copy(isImporting = true) }

                    val roughWorkDir = JavaFile("${applicationContext.filesDir}/" +
                            "$INTERNAL_ROUGH_WORK_RESTORE_DIRECTORY/")

                    val importDirectory = JavaFile(
                        parentFile = roughWorkDir,
                        child = action.directory.name,
                    )

                    roughWorkDir.file.deleteRecursively()

                    readFilesFromBackupUseCase.invoke(
                        exportDirectory = action.directory,
                        importDirectory = importDirectory,
                        getGenericFileForRepository = { relativePath ->
                            JavaFile(
                                parentFile = importDirectory,
                                child = relativePath,
                            )
                        }
                    )
                    action.onImportFinished()
                    _state.update { it.copy(isImporting = false) }
                }
            }
        }
    }
}