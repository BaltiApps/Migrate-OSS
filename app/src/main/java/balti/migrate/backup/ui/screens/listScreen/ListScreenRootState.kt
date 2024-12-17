package balti.migrate.backup.ui.screens.listScreen

data class ListScreenRootState(
    val isLoading: Boolean,
    val isStaging: Boolean,
    val currentRoute: RouteBackupList,
)
