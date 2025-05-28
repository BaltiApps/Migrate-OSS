package balti.migrate.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.isSystemInDarkTheme
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
import baltiapps.migrate.domain.common.sources.Preferences.DarkMode
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : AppCompatActivity() {

    private val viewModel: MainActivityViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setAppDarkMode(viewModel.darkMode.value)
        enableEdgeToEdge()
        setContent {
            MigrateTheme(
                darkTheme = when(viewModel.darkMode.value) {
                    DarkMode.LIGHT -> false
                    DarkMode.DARK -> true
                    DarkMode.SYSTEM -> isSystemInDarkTheme()
                },
                dynamicColor = viewModel.shouldFollowSystemColors.value,
            ) {
                Graph(
                    startBackupService = ::startBackupService,
                    cancelBackup = ::cancelBackup,
                    startRestoreService = ::startRestoreService,
                    cancelRestore = ::cancelRestore,
                    shouldShowPermissionScreen = viewModel::shouldShowPermissionScreen,
                    updateUiState = { darkMode, followSystemColors ->
                        setAppDarkMode(darkMode)
                        viewModel.updateUiState(darkMode, followSystemColors)
                    },
                )
            }
        }
    }

    private fun setAppDarkMode(darkMode: DarkMode) {
        println(darkMode)
        AppCompatDelegate.setDefaultNightMode(
            when(darkMode) {
                DarkMode.LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
                DarkMode.DARK -> AppCompatDelegate.MODE_NIGHT_YES
                DarkMode.SYSTEM -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            }
        )
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