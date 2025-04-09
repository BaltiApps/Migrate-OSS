package baltiapps.migrate.domain.restore.repository

import baltiapps.migrate.domain.BACKUP_FILE_NAME_CALL_LOGS
import baltiapps.migrate.domain.BACKUP_FILE_NAME_CONTACTS
import baltiapps.migrate.domain.BACKUP_FILE_NAME_SMS
import baltiapps.migrate.domain.common.model.GenericFile
import baltiapps.migrate.domain.common.repository.DataRepository

class RestoreDataRepository: DataRepository() {
    val backupFiles = mutableListOf<GenericFile>()

    fun getContactBackupFile(): GenericFile? = backupFiles.find { it.name == BACKUP_FILE_NAME_CONTACTS }
    fun getCallLogBackupFile(): GenericFile? = backupFiles.find { it.name == BACKUP_FILE_NAME_CALL_LOGS }
    fun getSmsBackupFile(): GenericFile? = backupFiles.find { it.name == BACKUP_FILE_NAME_SMS }
}