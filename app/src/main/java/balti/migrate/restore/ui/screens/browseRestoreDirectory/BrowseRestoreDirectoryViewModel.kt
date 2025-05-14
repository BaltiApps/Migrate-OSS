package balti.migrate.restore.ui.screens.browseRestoreDirectory

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import balti.migrate.R
import balti.migrate.common.data.model.JavaFile
import balti.migrate.common.data.model.MediaStoreDownloadFile
import balti.migrate.common.data.model.SafFile
import balti.migrate.common.data.sources.fileSystem.TransferUtils
import baltiapps.migrate.domain.INTERNAL_ROUGH_WORK_RESTORE_DIRECTORY
import baltiapps.migrate.domain.common.model.GenericFile
import baltiapps.migrate.domain.common.sources.Preferences
import baltiapps.migrate.domain.common.sources.fileSystem.ExportDirectoryBrowser
import baltiapps.migrate.domain.restore.usecase.ReadFilesFromBackupUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class BrowseRestoreDirectoryViewModel(
    private val exportDirectoryBrowserMediaStore: ExportDirectoryBrowser<GenericFile>,
    private val exportDirectoryBrowserSafFile: ExportDirectoryBrowser<GenericFile>,
    private val readFilesFromBackupUseCase: ReadFilesFromBackupUseCase,
    private val preferences: Preferences,
    private val applicationContext: Context,
): ViewModel() {

    private val basePath = MediaStoreDownloadFile.EXPORT_PATH_PREFIX

    private val safLocationString: String
        get() = preferences.getCustomLocationParameter()
    private val safLocationUri: Uri
        get() = safLocationString.toUri()
    private val isSafLocationAccessible: Boolean
        get() = TransferUtils.hasPermission(applicationContext, safLocationUri)

    private fun getInitialExportDirectory(): GenericFile {
        return if (!isSafLocationAccessible) {
            MediaStoreDownloadFile(path = basePath)
        } else {
            SafFile(
                uriToLocation = safLocationUri,
                name = "",
            )
        }
    }

    private fun getLocationLabel(file: GenericFile): String {
        return when(file) {
            is MediaStoreDownloadFile -> basePath
            is SafFile -> {
                TransferUtils.getSafFilePath(file).ifBlank {
                    TransferUtils.getSafFileName(applicationContext, file)
                }
            }
            else -> ""
        }
    }

    private fun getIsBackAllowed(file: GenericFile): Boolean {
        return when(file) {
            is MediaStoreDownloadFile -> file.path.startsWith(basePath) && file.path != basePath
            is SafFile -> file.parent != null
            else -> false
        }
    }

    private fun getParentFile(file: GenericFile): GenericFile? {
        return when(file) {
            is MediaStoreDownloadFile -> {
                val currentDirectoryPath = file.path
                val parentPath = currentDirectoryPath.substringBeforeLast("/")
                MediaStoreDownloadFile(path = parentPath)
            }
            is SafFile -> file.parent
            else -> null
        }
    }

    private fun getExportDirectoryBrowser(): ExportDirectoryBrowser<GenericFile> {
        return if (isSafLocationAccessible) {
            exportDirectoryBrowserSafFile
        } else {
            exportDirectoryBrowserMediaStore
        }
    }

    private val _state = MutableStateFlow(
        BrowseRestoreDirectoryState(
            isLoading = false,
            exportDirectoriesToShow = listOf(),
            currentExportDirectory = getInitialExportDirectory(),
            isImporting = false,
            isBackAllowed = false,
            isSaf = null,
            locationString = "",
            isSafUriAccessible = false,
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
            val subDirectoryList = getExportDirectoryBrowser().getDirectories(directory)
            _state.update {
                it.copy(
                    isLoading = false,
                    exportDirectoriesToShow = subDirectoryList,
                    currentExportDirectory = directory,
                    isBackAllowed = getIsBackAllowed(directory),
                    isSaf = safLocationString.isNotBlank(),
                    locationString = getLocationLabel(getInitialExportDirectory()),
                    isSafUriAccessible = isSafLocationAccessible,
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
                    val parentFile = getParentFile(_state.value.currentExportDirectory)
                    if (parentFile == null) {
                        Toast.makeText(applicationContext, R.string.no_parent, Toast.LENGTH_SHORT).show()
                        return@launch
                    }
                    loadDirectory(parentFile)
                }
                is BrowseRestoreDirectoryActions.OnSafLocationSelected -> {
                    if (action.uriString == null) {
                        return@launch
                    } else if (TransferUtils.hasPermission(applicationContext, action.uriString.toUri())) {
                        preferences.setCustomLocationParameter(action.uriString)
                        loadDirectory(getInitialExportDirectory())
                    } else {
                        Toast.makeText(applicationContext, R.string.no_parent, Toast.LENGTH_SHORT).show()
                    }
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