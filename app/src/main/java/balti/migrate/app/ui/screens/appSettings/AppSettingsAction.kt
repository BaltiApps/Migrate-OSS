package balti.migrate.app.ui.screens.appSettings

import baltiapps.migrate.domain.common.sources.Preferences

sealed class AppSettingsAction {
    data class ChangeDarkMode(
        val darkMode: Preferences.DarkMode,
        val updateUiState: (darkMode: Preferences.DarkMode, followSystemColors: Boolean) -> Unit,
    ) : AppSettingsAction()
    data class ChangeShouldFollowSystemColors(
        val shouldFollow: Boolean,
        val updateUiState: (darkMode: Preferences.DarkMode, followSystemColors: Boolean) -> Unit,
    ) : AppSettingsAction()
}