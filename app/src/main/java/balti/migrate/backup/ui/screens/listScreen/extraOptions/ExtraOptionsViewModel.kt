package balti.migrate.backup.ui.screens.listScreen.extraOptions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import baltiapps.migrate.domain.backup.repository.BackupDataRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ExtraOptionsViewModel(
    private val backupDataRepository: BackupDataRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ExtraOptionsState())
    val state = _state.asStateFlow()

    fun performAction(action: ExtraOptionsAction) = viewModelScope.launch(Dispatchers.Default) {
        when (action) {
            ExtraOptionsAction.RefreshCounts -> {
                _state.update { it.copy(isLoading = true) }
                val eligible = backupDataRepository.stagedApps.filter { it.toListItem().isDataSelected }
                val selected = eligible.count {
                    it.toListItem().isExternalDataSelected || it.toListItem().isExternalMediaSelected
                }
                _state.update {
                    it.copy(
                        isLoading = false,
                        externalDataTotalCount = eligible.size,
                        externalDataSelectedCount = selected,
                    )
                }
            }
        }
    }
}
