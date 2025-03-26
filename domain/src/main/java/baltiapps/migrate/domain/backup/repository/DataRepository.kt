package baltiapps.migrate.domain.backup.repository

import baltiapps.migrate.domain.backup.model.CallLogListItem
import baltiapps.migrate.domain.backup.model.ContactListItem
import baltiapps.migrate.domain.backup.model.DataItem
import baltiapps.migrate.domain.backup.model.Progress
import baltiapps.migrate.domain.backup.model.SmsListItem
import kotlinx.coroutines.flow.Flow

abstract class DataRepository {
    abstract val contactsDataItems: List<DataItem<ContactListItem>>
    abstract val callLogDataItems: List<DataItem<CallLogListItem>>
    abstract val smsDataItems: List<DataItem<SmsListItem>>

    protected abstract val stagedContacts: List<DataItem<ContactListItem>>
    protected abstract val stagedCallLogs: List<DataItem<CallLogListItem>>
    protected abstract val stagedSms: List<DataItem<SmsListItem>>

    abstract suspend fun readContactsFromDevice(): Flow<Progress>
    abstract suspend fun readCallLogsFromDevice(): Flow<Progress>
    abstract suspend fun readSmsFromDevice(): Flow<Progress>

    abstract fun setStagedContacts(ids: List<String>)
    abstract fun setStagedCallLogs(ids: List<String>)
    abstract fun setStagedSms(ids: List<String>)

    abstract fun backupContacts(backupRoot: String): Flow<Progress>
    abstract fun backupCalls(backupRoot: String): Flow<Progress>
    abstract fun backupSms(backupRoot: String): Flow<Progress>

    fun shouldBackupContacts(): Boolean {
        return stagedContacts.isNotEmpty()
    }
    fun shouldBackupCalls(): Boolean {
        return stagedCallLogs.isNotEmpty()
    }
    fun shouldBackupSms(): Boolean {
        return stagedSms.isNotEmpty()
    }

    protected suspend fun <T: DataItem<*>> collectData(
        getData: suspend ((List<T>) -> Unit) -> Flow<Progress>,
        collectorList: MutableList<T>
    ): Flow<Progress> {
        return getData { items ->
            collectorList.clear()
            collectorList.addAll(items)
        }
    }

    protected fun <T: DataItem<*>> setStagedItems(
        ids: List<String>,
        allDataItems: List<T>,
        stagedItemsCollector: MutableList<T>,
    ) {
        stagedItemsCollector.clear()
        stagedItemsCollector.addAll(allDataItems.filter { it._id in ids })
    }
}