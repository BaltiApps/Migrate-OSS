package balti.migrate.restore.ui.screens.progressScreen

import android.app.Activity

sealed class RestoreProgressScreenAction {
    data class ToggleErrorOnly(val enabled: Boolean) : RestoreProgressScreenAction()
    data object CancelRestore : RestoreProgressScreenAction()
    data object PauseProgressLogs : RestoreProgressScreenAction()
    data object ResumeProgressLogs : RestoreProgressScreenAction()
    data class ChangeSmsApp(val activity: Activity?): RestoreProgressScreenAction()
}