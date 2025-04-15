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

    override val contactsListItems: List<ContactListItem>
        get() = super.contactsListItems.map { it.copy(isChecked = it.isLocalContact) }
}