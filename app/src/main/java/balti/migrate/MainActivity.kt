package balti.migrate

import android.app.role.RoleManager
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import balti.migrate.backup.data.service.BackupService
import balti.migrate.common.data.model.JavaFile
import balti.migrate.common.ui.navigation.Graph
import balti.migrate.restore.data.service.RestoreService
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
import java.lang.ref.WeakReference

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MigrateTheme {
                Graph(
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
            PermissionConstants.SETTINGS_DEFAULT_APPS -> {
                specialPermissionLauncher.launch(
                    Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)
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