package balti.migrate.backup.ui.screens.progressScreen

sealed class BackupProgressScreenAction {
    data class ToggleErrorOnly(val enabled: Boolean) : BackupProgressScreenAction()
    data object CancelBackup : BackupProgressScreenAction()
    data object PauseProgressLogs : BackupProgressScreenAction()
    data object ResumeProgressLogs : BackupProgressScreenAction()
}