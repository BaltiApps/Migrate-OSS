package balti.migrate.app.ui.screens.appSettings

import baltiapps.migrate.domain.common.sources.Preferences

sealed class AppSettingsAction {
    data class ChangeDarkMode(
        val darkMode: Preferences.DarkMode,
        val onSetDarkMode: (Preferences.DarkMode) -> Unit,
    ) : AppSettingsAction()
}