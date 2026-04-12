package balti.migrate.app.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import balti.migrate.common.utils.SuperuserUtils
import baltiapps.migrate.domain.common.sources.Preferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ScreenHomeViewModel(
    private val preferences: Preferences,
    private val superuserUtils: SuperuserUtils,
): ViewModel() {

    private val _state = MutableStateFlow(
        ScreenHomeState(
            isRootEnabled = preferences.wasSuPermissionGranted(),
        )
    )
    val state = _state.asStateFlow()

    init {
        if (preferences.wasSuPermissionGranted()) {
            checkRootPermission()
        }
    }

    private fun checkRootPermission(onToggleRequest: Boolean = false) {
        viewModelScope.launch {
            _state.update { it.copy(isCheckingRootPermission = true) }
            val result = superuserUtils.checkSuperuserPermission()
            if (result.isSuccess) {
                preferences.setWasSuPermissionGranted(true)
                _state.update { it.copy(isRootEnabled = true, isCheckingRootPermission = false) }
            } else {
                preferences.setWasSuPermissionGranted(false)
                _state.update { it.copy(isRootEnabled = false, isCheckingRootPermission = false) }
            }
        }
    }

    fun performAction(action: ScreenHomeAction) {
        when (action) {
            is ScreenHomeAction.OnAboutButtonClicked -> {
                _state.update { it.copy(shouldShowAboutDialog = true) }
            }
            is ScreenHomeAction.OnAboutDialogDismissed -> {
                _state.update { it.copy(shouldShowAboutDialog = false) }
            }
            is ScreenHomeAction.OnRootSwitchToggled -> {
                if (action.enabled) {
                    checkRootPermission(onToggleRequest = true)
                } else {
                    preferences.setWasSuPermissionGranted(false)
                    _state.update { it.copy(isRootEnabled = false) }
                }
            }
        }
    }
}