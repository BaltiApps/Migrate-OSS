package balti.migrate.app.ui.navigation

import androidx.lifecycle.ViewModel
import baltiapps.migrate.domain.backup.repository.BackupDataRepository

class HomeGraphViewModel(
    private val backupRepository: BackupDataRepository,
) : ViewModel() {
    fun resetBackupRepository() {
        backupRepository.resetRepository()
    }
}