package balti.migrate.app.ui.screens.appSettings

import baltiapps.migrate.domain.common.sources.Preferences

data class AppSettingsState(
    val darkMode: Preferences.DarkMode,
    val shouldFollowSystemColors: Boolean,
)
