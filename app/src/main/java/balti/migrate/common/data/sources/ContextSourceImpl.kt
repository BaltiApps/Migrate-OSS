package balti.migrate.common.data.sources

import android.app.role.RoleManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Environment
import androidx.core.content.ContextCompat
import balti.migrate.R
import baltiapps.migrate.domain.PermissionConstants
import baltiapps.migrate.domain.common.model.Progress
import baltiapps.migrate.domain.common.sources.ContextSource

class ContextSourceImpl(private val context: Context): ContextSource {
    override fun getProgressTitle(progressType: Progress.ProgressType): String {
        return when (progressType) {
            Progress.ProgressType.CONTACTS_BACKUP -> context.getString(R.string.label_contacts_backup)
            Progress.ProgressType.CALL_LOG_BACKUP -> context.getString(R.string.label_call_log_backup)
            Progress.ProgressType.SMS_BACKUP -> context.getString(R.string.label_sms_backup)
            Progress.ProgressType.BACKUP_FINISHED -> context.getString(R.string.backup_finished)
            Progress.ProgressType.BACKUP_FINISHED_WITH_ERRORS -> context.getString(R.string.backup_finished_with_errors)
            Progress.ProgressType.BACKUP_CANCELLED -> context.getString(R.string.backup_cancelled)
            Progress.ProgressType.CALL_LOG_RESTORE -> context.getString(R.string.label_call_log_restore)
            Progress.ProgressType.SMS_RESTORE -> context.getString(R.string.label_sms_restore)
            else -> context.getString(R.string.loading)
        }
    }

    override fun checkPermission(permission: String): Boolean {
        return when (permission) {
            PermissionConstants.MANAGE_EXTERNAL_STORAGE -> checkAllFilesAccess()
            PermissionConstants.DEFAULT_SMS_APP -> checkDefaultSmsApp()
            else -> ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }
    }

    override fun checkPermissions(permissions: List<String>): Boolean {
        return permissions.map { ContextCompat.checkSelfPermission(context, it) }
            .all { it == PackageManager.PERMISSION_GRANTED }
    }

    private fun checkAllFilesAccess(): Boolean {
        return Environment.isExternalStorageManager()
    }

    private fun checkDefaultSmsApp(): Boolean {
        val roleManager = context.getSystemService(RoleManager::class.java)
        return roleManager.isRoleHeld(RoleManager.ROLE_SMS)
    }
}