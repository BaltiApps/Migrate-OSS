package balti.migrate.backup.ui.screens

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import balti.migrate.backup.ui.screens.backupName.BackupName
import balti.migrate.backup.ui.screens.listScreen.ListScreenRoot
import balti.migrate.backup.ui.screens.progressScreen.ProgressScreen
import baltiapps.migrate.domain.backup.model.BackupLocation
import kotlinx.serialization.Serializable

@Composable
fun ScreenBackup(
    parentNavController: NavHostController,
    startBackupService: (BackupLocation) -> Unit,
    cancelBackup: () -> Unit,
) {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = RouteListScreen,
    ) {
        composable<RouteListScreen> {
            ListScreenRoot(
                navigateUp = {
                    parentNavController.navigateUp()
                },
                goToBackupNameScreen = { navController.navigate(RouteBackupName) },
            )
        }
        composable<RouteBackupName> {
            BackupName(
                navigateUp = navController::navigateUp,
                goToNextScreen = {
                    startBackupService(it)
                    navController.navigate(RouteProgressScreen) {
                        popUpTo(RouteListScreen) {
                            inclusive = true
                        }
                    }
                }
            )
        }
        composable<RouteProgressScreen> {
            ProgressScreen(
                cancelBackup = cancelBackup,
                closeProgressScreen = {
                    parentNavController.navigateUp()
                },
            )
        }
    }
}

@Serializable
object RouteListScreen

@Serializable
object RouteBackupName

@Serializable
object RouteProgressScreen