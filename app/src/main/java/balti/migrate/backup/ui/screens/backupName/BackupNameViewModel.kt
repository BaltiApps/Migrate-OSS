package balti.migrate.backup.ui.screens.backupName

import android.content.Context
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import balti.migrate.common.data.sources.fileSystem.TransferUtils
import balti.migrate.common.utils.getDefaultBackupName
import baltiapps.migrate.domain.common.sources.Preferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class BackupNameViewModel(
    private val applicationContext: Context,
    private val preferences: Preferences,
) : ViewModel() {

    private val _state = MutableStateFlow(
        BackupNameState(
            backupName = getDefaultBackupName(),
        )
    )
    val state = _state.asStateFlow()

    init {
        val safUriString = preferences.getCustomLocationParameter().takeIf { it.isNotBlank() }
        val isSafUriAccessible = safUriString?.run {
            TransferUtils.hasPermission(applicationContext, this.toUri())
        }
        _state.update {
            it.copy(
                safUriString = safUriString,
                isSafUriAccessible = isSafUriAccessible,
            )
        }
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
                } else if (TransferUtils.hasPermission(applicationContext, action.uriString.toUri())) {
                    preferences.setCustomLocationParameter(action.uriString)
                    _state.update {
                        it.copy(
                            safUriString = action.uriString,
                            isSafUriAccessible = true,
                        )
                    }
                } else {
                    _state.update {
                        it.copy(
                            safUriString = null,
                            isSafUriAccessible = false,
                        )
                    }
                }
            }
        }
    }

}