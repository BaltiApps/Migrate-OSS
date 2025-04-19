package balti.migrate.restore.ui

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import balti.migrate.restore.ui.screens.browseRestoreDirectory.BrowseRestoreDirectory
import balti.migrate.restore.ui.screens.listScreen.callLogRestoreSelection.CallLogRestoreSelection
import balti.migrate.restore.ui.screens.listScreen.contactRestoreSelection.ContactRestoreSelection
import balti.migrate.restore.ui.screens.listScreen.smsRestoreSelection.SmsRestoreSelection
import balti.migrate.restore.ui.screens.restoreSummary.RestoreSummary
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
                onBackupSelected = {
                    navController.navigate(RouteContactRestoreSelection)
                }
            )
        }
        composable<RouteContactRestoreSelection> {
            ContactRestoreSelection(
                navigateUp = navController::navigateUp,
                goToNextScreen = {
                    navController.navigate(RouteCallLogRestoreSelection)
                }
            )
        }
        composable<RouteCallLogRestoreSelection> {
            CallLogRestoreSelection(
                navigateUp = navController::navigateUp,
                goToNextScreen = {
                    navController.navigate(RouteSmsRestoreSelection)
                }
            )
        }
        composable<RouteSmsRestoreSelection> {
            SmsRestoreSelection(
                navigateUp = navController::navigateUp,
                goToNextScreen = {
                    navController.navigate(RouteRestoreSummary)
                }
            )
        }
        composable<RouteRestoreSummary> {
            RestoreSummary(
                navigateUp = navController::navigateUp,
                startRestoreService = {},
            )
        }
    }
}

@Serializable
object RouteDirectorySelection

@Serializable
object RouteContactRestoreSelection

@Serializable
object RouteCallLogRestoreSelection

@Serializable
object RouteSmsRestoreSelection

@Serializable
object RouteRestoreSummary