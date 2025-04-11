package balti.migrate

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import balti.migrate.backup.data.service.BackupService
import balti.migrate.backup.ui.screens.ScreenBackup
import balti.migrate.restore.ui.ScreenRestore
import balti.migrate.ui.screens.ScreenHome
import balti.migrate.ui.theme.MigrateTheme
import baltiapps.migrate.domain.ACTION_START_BACKUP
import baltiapps.migrate.domain.ACTION_CANCEL_BACKUP
import baltiapps.migrate.domain.EXTRA_BACKUP_LOCATION
import baltiapps.migrate.domain.EXTRA_BACKUP_NAME
import baltiapps.migrate.domain.EXTRA_BACKUP_ROOT
import baltiapps.migrate.domain.backup.model.BackupLocation
import kotlinx.serialization.Serializable

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MigrateTheme {
                AppNavigation(
                    startBackupService = ::startBackupService,
                    cancelBackup = ::cancelBackup
                )
            }
        }
    }

    private fun startBackupService(backupLocation: BackupLocation) {
        Intent(this, BackupService::class.java).apply {
            action = ACTION_START_BACKUP
            putExtra(EXTRA_BACKUP_ROOT, backupLocation.getFullPath())
            putExtra(EXTRA_BACKUP_LOCATION, backupLocation.backupLocation)
            putExtra(EXTRA_BACKUP_NAME, backupLocation.backupName)
        }.run {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(this)
            } else {
                startService(this)
            }
        }
    }

    private fun cancelBackup() {
        Intent(this, BackupService::class.java).apply {
            action = ACTION_CANCEL_BACKUP
        }.run {
            startService(this)
        }
    }
}

@Composable
fun AppNavigation(
    startBackupService: (BackupLocation) -> Unit,
    cancelBackup: () -> Unit,
) {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = RouteHome,
    ) {
        composable<RouteHome> {
            ScreenHome(navController)
        }
        composable<RouteBackup> {
            ScreenBackup(
                parentNavControllerNavigateUp = navController::navigateUp,
                startBackupService = { startBackupService(it) },
                cancelBackup = cancelBackup,
            )
        }
        composable<RouteRestore> {
            ScreenRestore(
                parentNavControllerNavigateUp = navController::navigateUp,
                startRestoreService = {},
                cancelRestore = {},
            )
        }
    }
}

@Serializable
object RouteHome

@Serializable
object RouteBackup

@Serializable
object RouteRestore