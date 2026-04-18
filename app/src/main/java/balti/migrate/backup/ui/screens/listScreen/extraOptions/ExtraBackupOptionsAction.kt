package balti.migrate.backup.ui.screens.listScreen.extraOptions

sealed class ExtraBackupOptionsAction {
    data object RefreshCounts : ExtraBackupOptionsAction()
}
