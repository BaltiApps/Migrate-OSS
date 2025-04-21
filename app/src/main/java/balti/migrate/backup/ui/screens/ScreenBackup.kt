package balti.migrate.backup.ui.screens

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import balti.migrate.backup.ui.screens.backupName.BackupName
import balti.migrate.backup.ui.screens.listScreen.callLogBackupSelection.CallLogBackupSelection
import balti.migrate.backup.ui.screens.listScreen.contactBackupSelection.ContactBackupSelection
import balti.migrate.backup.ui.screens.listScreen.smsBackupSelection.SmsBackupSelection
import balti.migrate.backup.ui.screens.progressScreen.BackupProgressScreen
import baltiapps.migrate.domain.backup.model.BackupLocation
import kotlinx.serialization.Serializable

@Composable
fun ScreenBackup(
    parentNavControllerNavigateUp: () -> Unit,
    startBackupService: (BackupLocation) -> Unit,
    cancelBackup: () -> Unit,
) {
    val startDestination = RouteCallLogBackup
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = startDestination,
    ) {
        composable<RouteCallLogBackup> {
            CallLogBackupSelection(
                navigateUp = navController::navigateUp,
                goToNextScreen = {
                    navController.navigate(RouteSmsBackup)
                },
            )
        }
        composable<RouteSmsBackup> {
            SmsBackupSelection(
                navigateUp = navController::navigateUp,
                goToNextScreen = {
                    navController.navigate(RouteContactBackup)
                }
            )
        }
        composable<RouteContactBackup> {
            ContactBackupSelection(
                navigateUp = navController::navigateUp,
                goToNextScreen = {
                    navController.navigate(RouteBackupName)
                },
            )
        }
        composable<RouteBackupName> {
            BackupName(
                navigateUp = navController::navigateUp,
                goToNextScreen = {
                    startBackupService(it)
                    navController.navigate(RouteBackupProgressScreen) {
                        popUpTo(startDestination) {
                            inclusive = true
                        }
                    }
                }
            )
        }
        composable<RouteBackupProgressScreen> {
            BackupProgressScreen(
                cancelBackup = cancelBackup,
                closeProgressScreen = parentNavControllerNavigateUp,
            )
        }
    }
}

@Serializable
object RouteBackupName

@Serializable
object RouteBackupProgressScreen

@Serializable
object RouteContactBackup

@Serializable
object RouteCallLogBackup

@Serializable
object RouteSmsBackup