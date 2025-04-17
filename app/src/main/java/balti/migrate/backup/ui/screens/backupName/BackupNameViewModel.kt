package balti.migrate.backup.ui.screens.backupName

import androidx.lifecycle.ViewModel
import balti.migrate.MainActivity
import balti.migrate.common.utils.getDefaultBackupName
import baltiapps.migrate.domain.PermissionConstants
import baltiapps.migrate.domain.common.sources.ContextSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class BackupNameViewModel(
    private val contextSource: ContextSource,
) : ViewModel() {

    private val permission = PermissionConstants.MANAGE_EXTERNAL_STORAGE

    private fun hasPermission(): Boolean {
        return contextSource.checkPermission(permission)
    }

    private val _state = MutableStateFlow(
        BackupNameState(
            backupName = getDefaultBackupName(),
            hasPermission = hasPermission()
        )
    )
    val state = _state.asStateFlow()

    fun onAction(action: BackupNameAction) {
        when (action) {
            is BackupNameAction.RequestPermission -> {
                if (action.activity !is MainActivity) return
                action.activity.requestPermission(permission) {
                    _state.update { it.copy(hasPermission = hasPermission()) }
                }
            }
            is BackupNameAction.NameChanged -> {
                _state.update {
                    it.copy(backupName = action.name)
                }
            }
        }
    }

}