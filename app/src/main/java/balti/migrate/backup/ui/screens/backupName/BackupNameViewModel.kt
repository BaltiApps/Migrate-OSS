package balti.migrate.backup.ui.screens.backupName

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import balti.migrate.common.data.model.MediaStoreDownloadFile
import balti.migrate.common.data.model.SafFile
import balti.migrate.common.data.sources.fileSystem.TransferUtils
import balti.migrate.common.utils.getDefaultBackupName
import baltiapps.migrate.domain.common.sources.Preferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class BackupNameViewModel(
    private val applicationContext: Context,
    private val preferences: Preferences,
) : ViewModel() {

    private val safLocationString: String
        get() = preferences.getCustomLocationParameter()
    private val safLocationUri: Uri
        get() = safLocationString.toUri()
    private val isSafLocationAccessible: Boolean
        get() = TransferUtils.hasPermission(applicationContext, safLocationUri)
    
    private fun getLocationLabel(uriString: String): String {
        if (uriString.isBlank()) return MediaStoreDownloadFile.EXPORT_PATH_PREFIX

        val safFile = SafFile(
            uriToLocation = uriString.toUri(),
            name = "",
        )
        return TransferUtils.getSafFilePath(safFile).ifBlank {
            TransferUtils.getSafFileName(applicationContext, safFile)
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
        }
    }

}