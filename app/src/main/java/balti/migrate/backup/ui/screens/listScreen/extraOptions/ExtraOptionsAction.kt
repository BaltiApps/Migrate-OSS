package balti.migrate.backup.ui.screens.listScreen.extraOptions

sealed class ExtraOptionsAction {
    data object RefreshCounts : ExtraOptionsAction()
}
