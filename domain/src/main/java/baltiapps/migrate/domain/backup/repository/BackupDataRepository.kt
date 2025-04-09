package baltiapps.migrate.domain.backup.repository

import baltiapps.migrate.domain.common.repository.DataRepository

class BackupDataRepository: DataRepository() {
    fun shouldBackupContacts(): Boolean {
        return stagedContacts.isNotEmpty()
    }
    fun shouldBackupCallLogs(): Boolean {
        return stagedCallLogs.isNotEmpty()
    }
    fun shouldBackupSms(): Boolean {
        return stagedSms.isNotEmpty()
    }
}