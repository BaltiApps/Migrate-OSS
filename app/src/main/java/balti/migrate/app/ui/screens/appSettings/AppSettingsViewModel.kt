package balti.migrate.app.ui.screens.appSettings

import androidx.lifecycle.ViewModel
import baltiapps.migrate.domain.common.sources.Preferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class AppSettingsViewModel(
    private val preferences: Preferences,
): ViewModel() {

    private val _state = MutableStateFlow(
        AppSettingsState(
            darkMode = preferences.getDarkMode(),
        )
    )
    val state = _state.asStateFlow()

    fun onAction(action: AppSettingsAction) {
        when (action) {
            is AppSettingsAction.ChangeDarkMode -> {
                action.onSetDarkMode(action.darkMode)
                _state.update {
                    it.copy(darkMode = action.darkMode)
                }
            }
        }
    }
}