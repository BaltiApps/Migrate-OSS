package balti.migrate.backup.ui.screens.backupName

import baltiapps.migrate.domain.backup.model.BackupLocation

sealed class BackupNameAction {
    data class NameChanged(val name: String): BackupNameAction()
    data class OnSafLocationSelected(
        val uriString: String?,
    ): BackupNameAction()
    data class StartBackup(
        val startBackupMethod: (BackupLocation) -> Unit,
    ): BackupNameAction()
    data object DismissNoSpaceDialog: BackupNameAction()
}
