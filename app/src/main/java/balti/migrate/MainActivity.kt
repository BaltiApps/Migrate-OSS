package balti.migrate

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import balti.migrate.app.ui.navigation.Graph
import balti.migrate.app.ui.theme.MigrateTheme
import balti.migrate.backup.data.service.BackupService
import balti.migrate.restore.data.service.RestoreService
import baltiapps.migrate.domain.ACTION_CANCEL_BACKUP
import baltiapps.migrate.domain.ACTION_CANCEL_RESTORE
import baltiapps.migrate.domain.ACTION_START_BACKUP
import baltiapps.migrate.domain.ACTION_START_RESTORE
import baltiapps.migrate.domain.EXTRA_BACKUP_NAME
import baltiapps.migrate.domain.EXTRA_BACKUP_URI_STRING
import baltiapps.migrate.domain.backup.model.BackupLocation
import baltiapps.migrate.domain.common.sources.Preferences
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {

    val preferences: Preferences by inject()

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
                    shouldShowPermissionScreen = preferences::shouldShowPermissionScreen
                )
            }
        }
    }

    private fun startBackupService(backupLocation: BackupLocation) {
        Intent(this, BackupService::class.java).apply {
            action = ACTION_START_BACKUP
            putExtra(EXTRA_BACKUP_URI_STRING, backupLocation.backupUriString)
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