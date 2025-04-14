package balti.migrate

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.core.net.toUri
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import balti.migrate.backup.data.service.BackupService
import balti.migrate.backup.ui.screens.ScreenBackup
import balti.migrate.restore.ui.ScreenRestore
import balti.migrate.ui.screens.ScreenHome
import balti.migrate.ui.theme.MigrateTheme
import baltiapps.migrate.domain.ACTION_CANCEL_BACKUP
import baltiapps.migrate.domain.ACTION_START_BACKUP
import baltiapps.migrate.domain.EXTRA_BACKUP_LOCATION
import baltiapps.migrate.domain.EXTRA_BACKUP_NAME
import baltiapps.migrate.domain.PermissionConstants
import baltiapps.migrate.domain.backup.model.BackupLocation
import kotlinx.serialization.Serializable
import java.lang.ref.WeakReference

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

    private var onUserPermissionConfirmation: WeakReference<((Boolean) -> Unit)>? = null

    private val permissionLauncherMultiple =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grantMap ->
            val isGranted = grantMap.values.all { it }
            onUserPermissionConfirmation?.get()?.invoke(isGranted)
            onUserPermissionConfirmation = null
        }

    private val permissionLauncherSingle =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            onUserPermissionConfirmation?.get()?.invoke(isGranted)
            onUserPermissionConfirmation = null
        }

    private val specialPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { it ->
            val isGranted = it.resultCode == RESULT_OK
            onUserPermissionConfirmation?.get()?.invoke(isGranted)
            onUserPermissionConfirmation = null
        }

    fun requestPermissions(permissions: List<String>, onUserConfirmation: (Boolean) -> Unit) {
        onUserPermissionConfirmation = WeakReference(onUserConfirmation)
        permissionLauncherMultiple.launch(permissions.toTypedArray())
    }

    fun requestPermission(permission: String, onUserConfirmation: (Boolean) -> Unit) {
        onUserPermissionConfirmation = WeakReference(onUserConfirmation)
        when (permission) {
            PermissionConstants.MANAGE_EXTERNAL_STORAGE -> {
                specialPermissionLauncher.launch(
                    Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                        data = "package:$packageName".toUri()
                    }
                )
            }
            else -> {
                permissionLauncherSingle.launch(permission)
            }
        }
    }

    private fun startBackupService(backupLocation: BackupLocation) {
        Intent(this, BackupService::class.java).apply {
            action = ACTION_START_BACKUP
            putExtra(EXTRA_BACKUP_LOCATION, backupLocation.backupLocation)
            putExtra(EXTRA_BACKUP_NAME, backupLocation.backupName)
        }.run {
            startForegroundService(this)
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