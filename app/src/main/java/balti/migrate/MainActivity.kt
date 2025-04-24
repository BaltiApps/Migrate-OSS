package balti.migrate

import android.app.role.RoleManager
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navDeepLink
import balti.migrate.backup.data.service.BackupService
import balti.migrate.backup.ui.screens.ScreenBackup
import balti.migrate.backup.ui.screens.progressScreen.BackupProgressScreen
import balti.migrate.common.data.model.JavaFile
import balti.migrate.common.utils.DeepLinkUtils
import balti.migrate.restore.data.service.RestoreService
import balti.migrate.restore.ui.ScreenRestore
import balti.migrate.restore.ui.screens.progressScreen.RestoreProgressScreen
import balti.migrate.ui.screens.ScreenHome
import balti.migrate.ui.theme.MigrateTheme
import baltiapps.migrate.domain.ACTION_CANCEL_BACKUP
import baltiapps.migrate.domain.ACTION_CANCEL_RESTORE
import baltiapps.migrate.domain.ACTION_START_BACKUP
import baltiapps.migrate.domain.ACTION_START_RESTORE
import baltiapps.migrate.domain.EXTRA_BACKUP_LOCATION
import baltiapps.migrate.domain.EXTRA_BACKUP_NAME
import baltiapps.migrate.domain.PermissionConstants
import baltiapps.migrate.domain.backup.model.BackupLocation
import baltiapps.migrate.domain.common.model.GenericFile
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
                    cancelBackup = ::cancelBackup,
                    startRestoreService = ::startRestoreService,
                    cancelRestore = ::cancelRestore,
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
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            val isGranted = it.resultCode == RESULT_OK
            onUserPermissionConfirmation?.get()?.invoke(isGranted)
            onUserPermissionConfirmation = null
        }

    private val roleManager by lazy {
        getSystemService(RoleManager::class.java)
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
            PermissionConstants.DEFAULT_SMS_APP -> {
                specialPermissionLauncher.launch(
                    roleManager.createRequestRoleIntent(RoleManager.ROLE_SMS)
                )
            }
            else -> {
                permissionLauncherSingle.launch(permission)
            }
        }
    }

    fun launchContactChooser(
        vcfFile: GenericFile?,
        onUserConfirmation: (Boolean) -> Unit,
    ) {
        if (vcfFile is JavaFile) {
            onUserPermissionConfirmation = WeakReference{
                WeakReference(onUserConfirmation).get()?.invoke(true)
            }
            val uri = FileProvider.getUriForFile(
                this,
                BuildConfig.contentProviderAuthority,
                vcfFile.file,
            )
            try {
                specialPermissionLauncher.launch(
                    Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(uri, "text/x-vcard")
                        putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("text/x-vcard"))
                        flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                    }
                )
            } catch (e: ActivityNotFoundException) {
                Toast.makeText(
                    this,
                    R.string.no_application_available_to_restore_contacts,
                    Toast.LENGTH_LONG
                ).show()
                onUserConfirmation(false)
            } catch (e: Exception) {
                e.printStackTrace()
                onUserConfirmation(false)
            }
        } else {
            onUserConfirmation(false)
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

    private fun startRestoreService() {
        Intent(this, RestoreService::class.java).apply {
            action = ACTION_START_RESTORE
        }.run {
            startForegroundService(this)
        }
    }

    private fun cancelRestore() {
        Intent(this, RestoreService::class.java).apply {
            action = ACTION_CANCEL_RESTORE
        }.run {
            startService(this)
        }
    }
}

@Composable
fun AppNavigation(
    startBackupService: (BackupLocation) -> Unit,
    cancelBackup: () -> Unit,
    startRestoreService: () -> Unit,
    cancelRestore: () -> Unit,
) {
    val navController = rememberNavController()
    val activity = LocalActivity.current
    NavHost(
        navController = navController,
        startDestination = RouteHome,
    ) {
        composable<RouteHome> {
            ScreenHome(
                onBackupSelected = { navController.navigate(RouteBackup) },
                onRestoreSelected = { navController.navigate(RouteRestore) },
            )
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
                startRestoreService = startRestoreService,
                cancelRestore = cancelRestore,
            )
        }
        composable<DeepLinkRouteBackupProgress>(
            deepLinks = listOf(
                navDeepLink { uriPattern = DeepLinkUtils.MigrateUri.UriProgressBackup.uriString }
            )
        ) {
            BackupProgressScreen(
                cancelBackup = cancelBackup,
                closeProgressScreen = { (activity as? MainActivity)?.finish() },
            )
        }
        composable<DeepLinkRouteRestoreProgress>(
            deepLinks = listOf(
                navDeepLink { uriPattern = DeepLinkUtils.MigrateUri.UriProgressRestore.uriString }
            )
        ) {
            RestoreProgressScreen(
                cancelRestore = cancelRestore,
                closeProgressScreen = { (activity as? MainActivity)?.finish() },
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

@Serializable
object DeepLinkRouteBackupProgress

@Serializable
object DeepLinkRouteRestoreProgress