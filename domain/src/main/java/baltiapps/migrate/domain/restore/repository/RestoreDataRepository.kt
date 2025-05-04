package baltiapps.migrate.domain.restore.repository

import baltiapps.migrate.domain.BACKUP_FILE_NAME_CALL_LOGS
import baltiapps.migrate.domain.BACKUP_FILE_NAME_CONTACTS
import baltiapps.migrate.domain.BACKUP_FILE_NAME_SMS
import baltiapps.migrate.domain.common.model.GenericFile
import baltiapps.migrate.domain.common.repository.DataRepository

class RestoreDataRepository: DataRepository() {
    val backupFiles = mutableListOf<GenericFile>()
    var exportDirectory: GenericFile? = null
    private set

    fun setExportDirectory(exportDirectory: GenericFile) {
        this.exportDirectory = exportDirectory
    }

    fun shouldRestoreContacts(): Boolean {
        return stagedContacts.isNotEmpty()
    }
    fun shouldRestoreCallLogs(): Boolean {
        return stagedCallLogs.isNotEmpty()
    }
    fun shouldRestoreSms(): Boolean {
        return stagedSms.isNotEmpty()
    }

    fun getContactBackupFile(): GenericFile? = backupFiles.find { it.name == BACKUP_FILE_NAME_CONTACTS }
    fun getCallLogBackupFile(): GenericFile? = backupFiles.find { it.name == BACKUP_FILE_NAME_CALL_LOGS }
    fun getSmsBackupFile(): GenericFile? = backupFiles.find { it.name == BACKUP_FILE_NAME_SMS }
}