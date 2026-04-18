package baltiapps.migrate.domain.backup.repository

import baltiapps.migrate.domain.common.model.ContactListItem
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
    fun shouldBackupApps(): Boolean {
        return stagedApps.isNotEmpty()
    }

    fun shouldBackupExternalData(): Boolean {
        return stagedApps.any {
            it.toListItem().isExternalDataSelected || it.toListItem().isExternalMediaSelected
        }
    }

    fun shouldBackupAnything(): Boolean {
        return (shouldBackupContacts() || shouldBackupCallLogs() || shouldBackupSms() || shouldBackupApps())
    }

    override val contactsListItems: List<ContactListItem>
        get() = super.contactsListItems.map { it.copy(isChecked = it.isLocalContact) }

    override fun resetRepository() {
        super.resetRepository()
    }
}