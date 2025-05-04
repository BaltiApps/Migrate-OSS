package baltiapps.migrate.domain.common.utils

import baltiapps.migrate.domain.BACKUP_FILE_NAME_CALL_LOGS
import baltiapps.migrate.domain.BACKUP_FILE_NAME_CONTACTS
import baltiapps.migrate.domain.BACKUP_FILE_NAME_SMS
import baltiapps.migrate.domain.common.model.GenericFile

object BackupFilesUtils {
    fun shouldImportFile(file: GenericFile): Boolean {
        return when (file.name) {
            BACKUP_FILE_NAME_CONTACTS -> true
            BACKUP_FILE_NAME_CALL_LOGS -> true
            BACKUP_FILE_NAME_SMS -> true
            else -> false
        }
    }
    fun shouldImportFile(path: String): Boolean {
        val name = path.trimEnd('/').substringAfterLast('/')
        return when(name) {
            BACKUP_FILE_NAME_CONTACTS -> true
            BACKUP_FILE_NAME_CALL_LOGS -> true
            BACKUP_FILE_NAME_SMS -> true
            else -> false
        }
    }
}