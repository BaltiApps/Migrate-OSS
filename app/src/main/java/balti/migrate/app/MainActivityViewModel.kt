package balti.migrate.app

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import baltiapps.migrate.domain.common.sources.Preferences
import baltiapps.migrate.domain.common.sources.Preferences.DarkMode

class MainActivityViewModel(
    private val preferences: Preferences,
): ViewModel() {

    val darkMode = mutableStateOf(preferences.getDarkMode())

    fun setDarkMode(value: DarkMode) {
        preferences.setDarkMode(value)
        darkMode.value = value
    }

    fun shouldShowPermissionScreen(): Boolean {
        return preferences.shouldShowPermissionScreen()
    }
}