package balti.migrate.backup.ui.screens.backupName

import androidx.lifecycle.ViewModel
import balti.migrate.common.utils.getDefaultBackupName
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class BackupNameViewModel() : ViewModel() {

    private val _state = MutableStateFlow(
        BackupNameState(
            backupName = getDefaultBackupName(),
        )
    )
    val state = _state.asStateFlow()

    fun onAction(action: BackupNameAction) {
        when (action) {
            is BackupNameAction.NameChanged -> {
                _state.update {
                    it.copy(backupName = action.name)
                }
            }
        }
    }

}