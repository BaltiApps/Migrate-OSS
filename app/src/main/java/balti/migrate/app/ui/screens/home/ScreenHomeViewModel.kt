package balti.migrate.app.ui.screens.home

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class ScreenHomeViewModel: ViewModel() {

    private val _state = MutableStateFlow(ScreenHomeState())
    val state = _state.asStateFlow()

    fun performAction(action: ScreenHomeAction) {
        when (action) {
            is ScreenHomeAction.OnAboutButtonClicked -> {
                _state.update { it.copy(shouldShowAboutDialog = true) }
            }
            is ScreenHomeAction.OnAboutDialogDismissed -> {
                _state.update { it.copy(shouldShowAboutDialog = false) }
            }
        }
    }
}