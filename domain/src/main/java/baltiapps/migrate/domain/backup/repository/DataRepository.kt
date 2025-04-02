package baltiapps.migrate.domain.backup.repository

import baltiapps.migrate.domain.backup.model.CallLogListItem
import baltiapps.migrate.domain.backup.model.ContactListItem
import baltiapps.migrate.domain.backup.model.DataItem
import baltiapps.migrate.domain.backup.model.ListItem
import baltiapps.migrate.domain.backup.model.Progress
import baltiapps.migrate.domain.backup.model.SmsListItem
import baltiapps.migrate.domain.backup.toListItems
import baltiapps.migrate.domain.exceptions.UndefinedBackupDataTypeException
import kotlinx.coroutines.flow.Flow

abstract class DataRepository {
    abstract val contactsDataItems: List<DataItem<ContactListItem>>
    val callLogDataItems = mutableListOf<DataItem<CallLogListItem>>()
    val smsDataItems = mutableListOf<DataItem<SmsListItem>>()

    protected abstract val stagedContacts: List<DataItem<ContactListItem>>
    protected val stagedCallLogs = mutableListOf<DataItem<CallLogListItem>>()
    protected val stagedSms = mutableListOf<DataItem<SmsListItem>>()

    abstract suspend fun readContactsFromDevice(): Flow<Progress>

    abstract fun setStagedContacts(ids: List<String>)
    abstract fun setStagedCallLogs(ids: List<String>)
    abstract fun setStagedSms(ids: List<String>)

    fun retrieveStagedCallLogs() = stagedCallLogs
    fun retrieveStagedSms() = stagedSms

    @Suppress("UNCHECKED_CAST")
    inline fun <reified T: ListItem> storeDataItems(items: List<DataItem<T>>) {
        when (T::class) {
            ContactListItem::class -> contactsDataItems
            CallLogListItem::class -> callLogDataItems
            SmsListItem::class -> smsDataItems
            else -> null
        }?.let {
            it as? MutableList<DataItem<T>>
        }?.run {
            this.clear()
            this.addAll(items)
        }
    }

    @Suppress("UNCHECKED_CAST")
    inline fun <reified T: ListItem> getReadListItems(): List<T> {
        return when (T::class) {
            ContactListItem::class -> contactsDataItems.toListItems()
            CallLogListItem::class -> callLogDataItems.toListItems().sortedByDescending { it.creationDate.dateInLong }
            SmsListItem::class -> smsDataItems.toListItems().sortedByDescending { it.creationDate.dateInLong }
            else -> null
        }?.let {
            it as? List<T>
        }?: throw UndefinedBackupDataTypeException(T::class)
    }

    @Suppress("UNCHECKED_CAST")
    protected inline fun <reified T: ListItem> getStagedDataItems(): List<DataItem<T>> {
        return when (T::class) {
            ContactListItem::class -> stagedContacts
            CallLogListItem::class -> stagedCallLogs
            SmsListItem::class -> stagedSms
            else -> null
        }?.let {
            it as? List<DataItem<T>>
        }?: throw UndefinedBackupDataTypeException(T::class)
    }

    abstract fun backupContacts(backupRoot: String): Flow<Progress>

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