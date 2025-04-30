package balti.migrate.app.ui.screens.setupPermission

sealed class SetupPermissionScreenAction {
    data class OnCallLogPermissionsResult(val isGranted: Boolean): SetupPermissionScreenAction()
    data class OnSmsPermissionResult(val isGranted: Boolean): SetupPermissionScreenAction()
    data class OnContactsPermissionResult(val isGranted: Boolean): SetupPermissionScreenAction()
    data class OnNotificationPermissionResult(val isGranted: Boolean): SetupPermissionScreenAction()
    data object OnAllFilesAccessPermissionAction: SetupPermissionScreenAction()
    data class OnAllRuntimePermissionsResult(val isGranted: Boolean): SetupPermissionScreenAction()
    data class OnSkipClicked(val dontShowAgain: Boolean): SetupPermissionScreenAction()
}