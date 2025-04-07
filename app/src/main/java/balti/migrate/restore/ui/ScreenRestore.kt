package balti.migrate.restore.ui

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import balti.migrate.restore.ui.screens.browseRestoreDirectory.BrowseRestoreDirectory
import baltiapps.migrate.domain.common.model.Directory
import kotlinx.serialization.Serializable

@Composable
fun ScreenRestore(
    parentNavControllerNavigateUp: () -> Unit,
    startRestoreService: (Directory) -> Unit,
    cancelRestore: () -> Unit,
) {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = RouteDirectorySelection,
    ) {
        composable<RouteDirectorySelection> {
            BrowseRestoreDirectory(
                navigateUp = parentNavControllerNavigateUp,
                onBackupSelected = {}
            )
        }
    }
}

@Serializable
object RouteDirectorySelection