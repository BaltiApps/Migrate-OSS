package balti.migrate.app.ui.screens.setupPermission

data class SetupPermissionScreenState(
    val isCallLogPermissionsGranted: Boolean = false,
    val isSmsReadPermissionGranted: Boolean = false,
    val isContactsReadPermissionGranted: Boolean = false,
    val isNotificationPermissionGranted: Boolean = false,
) {
    val isAllPermissionsGranted: Boolean = isCallLogPermissionsGranted &&
            isSmsReadPermissionGranted &&
            isContactsReadPermissionGranted &&
            isNotificationPermissionGranted
}
