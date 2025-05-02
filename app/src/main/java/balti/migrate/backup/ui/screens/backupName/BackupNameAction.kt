package balti.migrate.backup.ui.screens.backupName

sealed class BackupNameAction {
    data class NameChanged(val name: String): BackupNameAction()
}