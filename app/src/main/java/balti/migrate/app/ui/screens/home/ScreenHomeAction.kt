package balti.migrate.app.ui.screens.home

sealed class ScreenHomeAction {
    data object OnAboutButtonClicked: ScreenHomeAction()
    data object OnAboutDialogDismissed: ScreenHomeAction()
}