package balti.migrate.app.ui.screens.setupPermission

sealed class SetupPermissionScreenAction {
    data class OnCallLogPermissionsResult(val isGranted: Boolean): SetupPermissionScreenAction()
    data class OnSmsPermissionResult(val isGranted: Boolean): SetupPermissionScreenAction()
    data class OnContactsPermissionResult(val isGranted: Boolean): SetupPermissionScreenAction()
    data class OnNotificationPermissionResult(val isGranted: Boolean): SetupPermissionScreenAction()
    data object OnAllPermissionsGranted: SetupPermissionScreenAction()
    data class OnAllPermissionsResult(val isGranted: Boolean): SetupPermissionScreenAction()
    data class OnSkipClicked(val dontShowAgain: Boolean): SetupPermissionScreenAction()
}