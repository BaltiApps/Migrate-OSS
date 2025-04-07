package baltiapps.migrate.domain.restore.repository

import baltiapps.migrate.domain.BACKUP_FILE_NAME_CALL_LOGS
import baltiapps.migrate.domain.BACKUP_FILE_NAME_CONTACTS
import baltiapps.migrate.domain.BACKUP_FILE_NAME_SMS
import baltiapps.migrate.domain.common.model.CallLogListItem
import baltiapps.migrate.domain.common.model.ContactListItem
import baltiapps.migrate.domain.common.model.SmsListItem
import baltiapps.migrate.domain.common.model.DataItem
import baltiapps.migrate.domain.common.model.GenericFile
import baltiapps.migrate.domain.common.toListItems

class RestoreDataRepository {
    val backupFiles = mutableListOf<GenericFile>()

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