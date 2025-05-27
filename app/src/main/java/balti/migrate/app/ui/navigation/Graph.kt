package balti.migrate.app.ui.navigation

import android.widget.Toast
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navDeepLink
import balti.migrate.R
import balti.migrate.app.ui.screens.home.ScreenHome
import balti.migrate.app.ui.screens.setupPermission.SetupPermissionScreen
import balti.migrate.backup.data.service.BackupService
import balti.migrate.backup.ui.screens.backupName.BackupName
import balti.migrate.backup.ui.screens.listScreen.callLogBackupSelection.CallLogBackupSelection
import balti.migrate.backup.ui.screens.listScreen.contactBackupSelection.ContactBackupSelection
import balti.migrate.backup.ui.screens.listScreen.smsBackupSelection.SmsBackupSelection
import balti.migrate.backup.ui.screens.progressScreen.BackupProgressScreen
import balti.migrate.common.utils.DeepLinkUtils
import balti.migrate.restore.data.service.RestoreService
import balti.migrate.restore.ui.screens.browseRestoreDirectory.BrowseRestoreDirectory
import balti.migrate.restore.ui.screens.listScreen.callLogRestoreSelection.CallLogRestoreSelection
import balti.migrate.restore.ui.screens.listScreen.contactRestoreSelection.ContactRestoreSelection
import balti.migrate.restore.ui.screens.listScreen.smsRestoreSelection.SmsRestoreSelection
import balti.migrate.restore.ui.screens.progressScreen.RestoreProgressScreen
import balti.migrate.restore.ui.screens.restoreSummary.RestoreSummary
import baltiapps.migrate.domain.backup.model.BackupLocation
import kotlinx.serialization.Serializable
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun Graph(
    startBackupService: (BackupLocation) -> Unit,
    cancelBackup: () -> Unit,
    startRestoreService: () -> Unit,
    cancelRestore: () -> Unit,
    shouldShowPermissionScreen: () -> Boolean,
    modifier: Modifier = Modifier,
) {
    val navController = rememberNavController()
    val activity = LocalActivity.current
    NavHost(
        navController = navController,
        startDestination = if (shouldShowPermissionScreen()) RoutePermissionScreen else RouteHome,
        modifier = modifier,
    ) {
        composable<RoutePermissionScreen> {
            SetupPermissionScreen(
                goToNextScreen = {
                    navController.popBackStack()
                    navController.navigate(RouteHome)
                }
            )
        }
        composable<RouteHome> {
            val viewModel = it.getSharedViewModel<HomeGraphViewModel>(navController)
            ScreenHome(
                onBackupSelected = {
                    when {
                        BackupService.isRunning -> navController.navigate(RouteBackupProgressScreen)
                        RestoreService.isRunning -> {
                            Toast.makeText(
                                activity,
                                R.string.cannot_backup_when_restore_is_running,
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        else -> {
                            viewModel.resetBackupRepository()
                            navController.navigate(RouteBackup)
                        }
                    }
                },
                onRestoreSelected = {
                    when {
                        RestoreService.isRunning -> navController.navigate(RouteRestoreProgressScreen)
                        BackupService.isRunning -> {
                            Toast.makeText(
                                activity,
                                R.string.cannot_restore_when_backup_is_running,
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        else -> navController.navigate(RouteRestore)
                    }
                },
            )
        }
        navigation<RouteBackup>(
            startDestination = RouteBackupSelections,
        ) {
            navigation<RouteBackupSelections>(
                startDestination = RouteCallLogBackup
            ){
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
                            navController.navigate(RouteBackupProgressScreen)
                        }
                    )
                }
            }
            composable<RouteBackupProgressScreen>(
                deepLinks = listOf(
                    navDeepLink { uriPattern = DeepLinkUtils.MigrateUri.UriProgressBackup.uriString }
                )
            ) {
                val isDeepLinked =
                    activity?.intent?.data?.toString() == DeepLinkUtils.MigrateUri.UriProgressBackup.uriString
                BackupProgressScreen(
                    cancelBackup = cancelBackup,
                    closeProgressScreen = {
                        navController.popBackStack(RouteBackup, true)
                        if (isDeepLinked) {
                            activity.finish()
                        }
                    },
                )
            }
        }
        navigation<RouteRestore>(
            startDestination = RouteRestoreSelections
        ) {
            navigation<RouteRestoreSelections>(
                startDestination = RouteDirectorySelection
            ) {
                composable<RouteDirectorySelection> {
                    val viewModel = it.getSharedViewModel<RestoreRouteChoicesViewModel>(navController)
                    BrowseRestoreDirectory(
                        navigateUp = navController::navigateUp,
                        onBackupSelected = {
                            navController.navigate(viewModel.findNextRoute())
                        }
                    )
                }
                composable<RouteCallLogRestoreSelection> {
                    val viewModel = it.getSharedViewModel<RestoreRouteChoicesViewModel>(navController)
                    CallLogRestoreSelection(
                        navigateUp = {
                            viewModel.onBackFromRoute()
                            navController.navigateUp()
                        },
                        goToNextScreen = {
                            navController.navigate(viewModel.findNextRoute())
                        }
                    )
                }
                composable<RouteSmsRestoreSelection> {
                    val viewModel = it.getSharedViewModel<RestoreRouteChoicesViewModel>(navController)
                    SmsRestoreSelection(
                        navigateUp = {
                            viewModel.onBackFromRoute()
                            navController.navigateUp()
                        },
                        goToNextScreen = {
                            navController.navigate(viewModel.findNextRoute())
                        }
                    )
                }
                composable<RouteContactRestoreSelection> {
                    val viewModel = it.getSharedViewModel<RestoreRouteChoicesViewModel>(navController)
                    ContactRestoreSelection(
                        navigateUp = {
                            viewModel.onBackFromRoute()
                            navController.navigateUp()
                        },
                        goToNextScreen = {
                            navController.navigate(viewModel.findNextRoute())
                        }
                    )
                }
                composable<RouteRestoreSummary> {
                    val viewModel = it.getSharedViewModel<RestoreRouteChoicesViewModel>(navController)
                    RestoreSummary(
                        navigateUp = {
                            viewModel.onBackFromRoute()
                            navController.navigateUp()
                        },
                        startRestoreServiceAndGoToNextScreen = {
                            startRestoreService()
                            navController.navigate(RouteRestoreProgressScreen)
                        },
                    )
                }
            }
            composable<RouteRestoreProgressScreen>(
                deepLinks = listOf(
                    navDeepLink { uriPattern = DeepLinkUtils.MigrateUri.UriProgressRestore.uriString }
                )
            ) {
                val isDeepLinked =
                activity?.intent?.data?.toString() == DeepLinkUtils.MigrateUri.UriProgressRestore.uriString
                RestoreProgressScreen(
                    cancelRestore = cancelRestore,
                    closeProgressScreen = {
                        navController.popBackStack(RouteRestore, true)
                        if (isDeepLinked) {
                            activity.finish()
                        }
                    },
                )
            }
        }
    }
}

@Serializable
object RoutePermissionScreen

@Serializable
object RouteHome

@Serializable
object RouteBackup

@Serializable
object RouteBackupSelections

@Serializable
object RouteCallLogBackup

@Serializable
object RouteSmsBackup

@Serializable
object RouteContactBackup

@Serializable
object RouteBackupName

@Serializable
object RouteBackupProgressScreen

@Serializable
object RouteRestore

@Serializable
object RouteRestoreSelections

@Serializable
object RouteDirectorySelection

@Serializable
object RouteCallLogRestoreSelection

@Serializable
object RouteSmsRestoreSelection

@Serializable
object RouteContactRestoreSelection

@Serializable
object RouteRestoreSummary

@Serializable
object RouteRestoreProgressScreen

@Composable
inline fun <reified T: ViewModel> NavBackStackEntry.getSharedViewModel(navController: NavController): T {
    val parentRoute = this.destination.parent?.route ?: return koinViewModel()
    val parentEntry = remember(this) {
        navController.getBackStackEntry(parentRoute)
    }
    return koinViewModel(viewModelStoreOwner = parentEntry)
}