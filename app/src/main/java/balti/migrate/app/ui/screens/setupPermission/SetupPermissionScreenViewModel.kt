package balti.migrate.app.ui.screens.setupPermission

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import balti.migrate.common.utils.PermissionUtils
import balti.migrate.common.utils.SuperuserUtils
import baltiapps.migrate.domain.common.sources.ContextSource
import baltiapps.migrate.domain.common.sources.Preferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SetupPermissionScreenViewModel(
    private val contextSource: ContextSource,
    private val preferences: Preferences,
    private val superuserUtils: SuperuserUtils,
) : ViewModel() {

    private val _state = MutableStateFlow(SetupPermissionScreenState())
    val state = _state.asStateFlow()

    private fun updateState(isGranted: Boolean? = null) {
        _state.update {
            it.copy(
                isCallLogPermissionsGranted = isGranted ?: contextSource.checkPermissions(
                    PermissionUtils.callLogPermissions
                ),
                isSmsReadPermissionGranted = isGranted ?: contextSource.checkPermission(
                    PermissionUtils.smsReadPermission
                ),
                isContactsReadPermissionGranted = isGranted ?: contextSource.checkPermission(
                    PermissionUtils.contactsReadPermission
                ),
                isNotificationPermissionGranted = isGranted
                    ?: PermissionUtils.notificationsPermission?.run {
                        contextSource.checkPermission(this)
                    } ?: true,
            )
        }
        if (preferences.wasSuperuserPermissionPreviouslyGranted()) {
            refreshSuperuserPermission()
        }
    }

    private fun refreshSuperuserPermission(
        onSuError: ((error: String) -> Unit)? = null,
    ) {
        viewModelScope.launch {
            _state.update { it.copy(isAskingSuperuserPermission = true) }
            val isSuGranted = superuserUtils.checkSuperuserPermission()
            preferences.setSuperuserPermissionPreviouslyGranted(isSuGranted.isSuccess)
            if (isSuGranted.isFailure) {
                onSuError?.invoke(isSuGranted.exceptionOrNull()?.message ?: "Unknown error")
            }
            _state.update {
                it.copy(
                    isAskingSuperuserPermission = false,
                    isSuperuserPermissionGranted = isSuGranted.isSuccess
                )
            }
        }
    }

    init {
        updateState()
    }

    fun performAction(action: SetupPermissionScreenAction) {
        when (action) {
            is SetupPermissionScreenAction.OnCallLogPermissionsResult -> {
                _state.update { it.copy(isCallLogPermissionsGranted = action.isGranted) }
            }
            is SetupPermissionScreenAction.OnSmsPermissionResult -> {
                _state.update { it.copy(isSmsReadPermissionGranted = action.isGranted) }
            }
            is SetupPermissionScreenAction.OnContactsPermissionResult -> {
                _state.update { it.copy(isContactsReadPermissionGranted = action.isGranted) }
            }
            is SetupPermissionScreenAction.OnNotificationPermissionResult -> {
                _state.update { it.copy(isNotificationPermissionGranted = action.isGranted) }
            }
            is SetupPermissionScreenAction.OnAllPermissionsResult -> {
                if (action.isGranted) {
                    updateState(isGranted = true)
                } else updateState()
            }
            is SetupPermissionScreenAction.OnAllPermissionsGranted -> {
                preferences.setShouldShowPermissionScreen(false)
            }
            is SetupPermissionScreenAction.OnSkipClicked -> {
                if (action.dontShowAgain) {
                    preferences.setShouldShowPermissionScreen(false)
                }
            }
            is SetupPermissionScreenAction.CheckSuperuserPermission -> {
                refreshSuperuserPermission(action.onError)
            }
        }
    }

}