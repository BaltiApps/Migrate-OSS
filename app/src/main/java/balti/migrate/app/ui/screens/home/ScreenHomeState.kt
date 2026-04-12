package balti.migrate.app.ui.screens.home

data class ScreenHomeState(
    val shouldShowAboutDialog: Boolean = false,
    val isRootEnabled: Boolean = false,
    val isCheckingRootPermission: Boolean = false,
)