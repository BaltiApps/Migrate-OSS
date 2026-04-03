package baltiapps.migrate.domain.common.repository

import baltiapps.migrate.domain.common.model.AppListItem
import baltiapps.migrate.domain.common.model.CallLogListItem
import baltiapps.migrate.domain.common.model.ContactListItem
import baltiapps.migrate.domain.common.model.DataItem
import baltiapps.migrate.domain.common.model.SmsListItem
import baltiapps.migrate.domain.common.toListItems

abstract class DataRepository {
    val contactsDataItems = mutableListOf<DataItem<ContactListItem>>()
    val callLogDataItems = mutableListOf<DataItem<CallLogListItem>>()
    val smsDataItems = mutableListOf<DataItem<SmsListItem>>()

    val appDataItems = mutableListOf<DataItem<AppListItem>>()

    val stagedContacts = mutableListOf<DataItem<ContactListItem>>()
    val stagedCallLogs = mutableListOf<DataItem<CallLogListItem>>()
    val stagedSms = mutableListOf<DataItem<SmsListItem>>()

    val stagedApps = mutableListOf<DataItem<AppListItem>>()

    open val contactsListItems: List<ContactListItem>
        get() = contactsDataItems.toListItems().sortedBy { it.displayName }

    open val callLogListItems: List<CallLogListItem>
        get() = callLogDataItems.toListItems().sortedByDescending { it.creationDate.dateInLong }

    open val smsListItems: List<SmsListItem>
        get() = smsDataItems.toListItems().sortedByDescending { it.creationDate.dateInLong }

    open val appListItems: List<AppListItem>
        get() = appDataItems.toListItems().sortedBy { it.appName }

    open fun resetRepository() {
        contactsDataItems.clear()
        callLogDataItems.clear()
        smsDataItems.clear()

        appDataItems.clear()

        stagedContacts.clear()
        stagedCallLogs.clear()
        stagedSms.clear()

        stagedApps.clear()
    }
}
