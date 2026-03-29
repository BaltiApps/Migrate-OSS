package balti.migrate.backup.ui.screens.backupName

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import balti.migrate.common.data.model.MediaStoreDownloadFile
import balti.migrate.common.data.sources.fileSystem.TransferUtils
import balti.migrate.common.utils.getDefaultBackupName
import baltiapps.migrate.domain.backup.model.BackupLocation
import baltiapps.migrate.domain.backup.repository.BackupDataRepository
import baltiapps.migrate.domain.common.sources.Preferences
import baltiapps.migrate.domain.restore.usecase.CalculateStagedAppsSizesUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class BackupNameViewModel(
    private val applicationContext: Context,
    private val preferences: Preferences,
    private val calculateStagedAppsSizesUseCase: CalculateStagedAppsSizesUseCase,
    private val backupDataRepository: BackupDataRepository,
) : ViewModel() {

    private val safLocationString: String
        get() = preferences.getCustomLocationParameter()
    private val safLocationUri: Uri
        get() = safLocationString.toUri()
    private val isSafLocationAccessible: Boolean
        get() = TransferUtils.hasPermission(applicationContext, safLocationUri)
    
    private fun getLocationLabel(uriString: String): String {
        if (uriString.isBlank()) return MediaStoreDownloadFile.EXPORT_PATH_PREFIX

        return TransferUtils.getUriFilePath(safLocationUri).ifBlank {
            TransferUtils.getUriFileName(applicationContext, safLocationUri)
        }
    }

    private val _state = MutableStateFlow(
        BackupNameState(
            backupName = getDefaultBackupName(),
            isSaf = null,
            safUriString = null,
            locationString = "",
            isSafUriAccessible = false,
        )
    )
    val state = _state.asStateFlow()

    private fun updateState() {
        viewModelScope.launch {
            _state.update {
                it.copy(
                    isSaf = safLocationString.isNotBlank(),
                    safUriString = safLocationString.takeIf {
                        it.isNotBlank() && isSafLocationAccessible
                    },
                    locationString = getLocationLabel(safLocationString),
                    isSafUriAccessible = isSafLocationAccessible,
                )
            }
        }
    }

    init {
        updateState()
    }

    fun onAction(action: BackupNameAction) {
        when (action) {
            is BackupNameAction.NameChanged -> {
                _state.update {
                    it.copy(backupName = action.name)
                }
            }
            is BackupNameAction.OnSafLocationSelected -> {
                if (action.uriString == null) {
                    return
                } else {
                    preferences.setCustomLocationParameter(action.uriString)
                    updateState()
                }
            }
            is BackupNameAction.StartBackup -> {
                startBackup(action.startBackupMethod)
            }
        }
    }

    private fun startBackup(startBackupMethod: (BackupLocation) -> Unit) {
        viewModelScope.launch {
            if (backupDataRepository.shouldBackupApps()) {
                _state.update { it.copy(isScanningAppSizes = true) }
                calculateStagedAppsSizesUseCase().onCompletion {
                    _state.update {
                        it.copy(
                            appSizeScanProgress = it.appSizeScanProgress?.copy(percentage = 1.0),
                            appSizes = backupDataRepository.stagedAppSizeMap,
                        )
                    }
                }.collect { progress ->
                    _state.update { it.copy(appSizeScanProgress = progress) }
                }
                _state.update { it.copy(isScanningAppSizes = false) }
            }
            startBackupMethod(
                BackupLocation(
                    backupName = state.value.backupName,
                    backupUriString = state.value.safUriString,
                )
            )
        }
    }

}
