package balti.migrate.backup.ui.screens.backupName

import android.app.Activity

sealed class BackupNameAction {
    data class RequestPermission(val activity: Activity?): BackupNameAction()
    data class NameChanged(val name: String): BackupNameAction()
}