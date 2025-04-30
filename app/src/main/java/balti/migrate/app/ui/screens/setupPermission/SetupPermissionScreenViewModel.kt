package balti.migrate.app.ui.screens.setupPermission

import androidx.lifecycle.ViewModel
import baltiapps.migrate.domain.common.sources.ContextSource
import baltiapps.migrate.domain.common.sources.Preferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class SetupPermissionScreenViewModel(
    private val contextSource: ContextSource,
    private val preferences: Preferences,
) : ViewModel() {

    private val _state = MutableStateFlow(SetupPermissionScreenState())
    val state = _state.asStateFlow()

    private fun updateState() {
        _state.update {
            it.copy(
                isCallLogPermissionsGranted = contextSource.checkPermissions(PermissionUtils.callLogPermissions),
                isSmsReadPermissionGranted = contextSource.checkPermission(PermissionUtils.smsReadPermission),
                isContactsReadPermissionGranted = contextSource.checkPermission(PermissionUtils.contactsReadPermission),
                isNotificationPermissionGranted = PermissionUtils.notificationsPermission?.run {
                    contextSource.checkPermission(this)
                } ?: true,
                isAllFilesAccessGranted = contextSource.checkPermission(PermissionUtils.allFilesAccessPermissionConstant),
            )
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
            is SetupPermissionScreenAction.OnAllFilesAccessPermissionAction -> {
                _state.update {
                    it.copy(
                        shouldAskAllFilesAccess = false,
                        isAllFilesAccessGranted = contextSource.checkPermission(
                            PermissionUtils.allFilesAccessPermissionConstant
                        )
                    )
                }
            }
            is SetupPermissionScreenAction.OnAllRuntimePermissionsResult -> {
                _state.update {
                    it.copy(shouldAskAllFilesAccess = true)
                }
                updateState()
            }
            is SetupPermissionScreenAction.OnAllPermissionsGranted -> {
                preferences.setShouldShowPermissionScreen(false)
            }
            is SetupPermissionScreenAction.OnSkipClicked -> {
                if (action.dontShowAgain) {
                    preferences.setShouldShowPermissionScreen(false)
                }
            }
        }
    }

}