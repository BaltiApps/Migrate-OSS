package balti.migrate.backup.ui.screens.progressScreen

sealed class ProgressScreenAction {
    class ToggleErrorOnly(val enabled: Boolean) : ProgressScreenAction()
    data object CancelBackup : ProgressScreenAction()
    data object PauseProgressLogs : ProgressScreenAction()
    data object ResumeProgressLogs : ProgressScreenAction()
}