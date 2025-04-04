package baltiapps.migrate.domain.backup.repository

import baltiapps.migrate.domain.common.model.CallLogListItem
import baltiapps.migrate.domain.common.model.ContactListItem
import baltiapps.migrate.domain.common.model.DataItem
import baltiapps.migrate.domain.common.model.SmsListItem
import baltiapps.migrate.domain.common.toListItems

class BackupDataRepository {
    val contactsDataItems = mutableListOf<DataItem<ContactListItem>>()
    val callLogDataItems = mutableListOf<DataItem<CallLogListItem>>()
    val smsDataItems = mutableListOf<DataItem<SmsListItem>>()

    val stagedContacts = mutableListOf<DataItem<ContactListItem>>()
    val stagedCallLogs = mutableListOf<DataItem<CallLogListItem>>()
    val stagedSms = mutableListOf<DataItem<SmsListItem>>()

    val contactsListItems: List<ContactListItem>
        get() = contactsDataItems.toListItems().sortedBy { it.displayName }

    val callLogListItems: List<CallLogListItem>
        get() = callLogDataItems.toListItems().sortedByDescending { it.creationDate.dateInLong }

    val smsListItems: List<SmsListItem>
        get() = smsDataItems.toListItems().sortedByDescending { it.creationDate.dateInLong }

    fun shouldBackupContacts(): Boolean {
        return stagedContacts.isNotEmpty()
    }
    fun shouldBackupCalls(): Boolean {
        return stagedCallLogs.isNotEmpty()
    }
    fun shouldBackupSms(): Boolean {
        return stagedSms.isNotEmpty()
    }
}