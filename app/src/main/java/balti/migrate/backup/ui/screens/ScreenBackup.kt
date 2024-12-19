package balti.migrate.backup.ui.screens

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.Composable
import androidx.navigation.NavOptions
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import balti.migrate.backup.ui.screens.backupName.BackupName
import balti.migrate.backup.ui.screens.listScreen.callLogBackup.CallLogBackup
import balti.migrate.backup.ui.screens.listScreen.contactBackup.ContactBackup
import balti.migrate.backup.ui.screens.listScreen.smsBackup.SmsBackup
import balti.migrate.backup.ui.screens.progressScreen.ProgressScreen
import baltiapps.migrate.domain.backup.model.BackupLocation
import kotlinx.serialization.Serializable

@Composable
fun ScreenBackup(
    parentNavControllerNavigateUp: () -> Unit,
    startBackupService: (BackupLocation) -> Unit,
    cancelBackup: () -> Unit,
) {
    val startDestination = RouteContactBackup
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = startDestination,
    ) {
        composable<RouteContactBackup>(
            exitTransition = { ExitTransition.None },
        ) {
            ContactBackup(
                navigateUp = navController::navigateUp,
                goToNextScreen = {
                    navController.navigate(RouteCallLogBackup)
                },
            )
        }
        composable<RouteCallLogBackup>(
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None },
        ) {
            CallLogBackup(
                navigateUp = navController::navigateUp,
                goToNextScreen = {
                    navController.navigate(RouteSmsBackup)
                },
            )
        }
        composable<RouteSmsBackup>(
            enterTransition = { EnterTransition.None },
        ) {
            SmsBackup(
                navigateUp = navController::navigateUp,
                goToNextScreen = {
                    navController.navigate(RouteBackupName)
                }
            )
        }
        composable<RouteBackupName> {
            BackupName(
                navigateUp = navController::navigateUp,
                goToNextScreen = {
                    startBackupService(it)
                    navController.navigate(RouteProgressScreen) {
                        popUpTo(startDestination) {
                            inclusive = true
                        }
                    }
                }
            )
        }
        composable<RouteProgressScreen> {
            ProgressScreen(
                cancelBackup = cancelBackup,
                closeProgressScreen = parentNavControllerNavigateUp,
            )
        }
    }
}

@Serializable
object RouteBackupName

@Serializable
object RouteProgressScreen

@Serializable
object RouteContactBackup

@Serializable
object RouteCallLogBackup

@Serializable
object RouteSmsBackup