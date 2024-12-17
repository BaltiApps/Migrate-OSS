package balti.migrate.backup.data.repository

import balti.migrate.backup.data.model.CallLogData
import balti.migrate.backup.data.model.ContactData
import balti.migrate.backup.data.model.SmsData
import baltiapps.migrate.domain.BACKUP_FILE_NAME_CALL_LOGS
import baltiapps.migrate.domain.BACKUP_FILE_NAME_CONTACTS
import baltiapps.migrate.domain.BACKUP_FILE_NAME_SMS
import baltiapps.migrate.domain.backup.getPercentage
import baltiapps.migrate.domain.backup.model.DataItem
import baltiapps.migrate.domain.backup.model.Progress
import baltiapps.migrate.domain.backup.repository.PlatformDataRepository
import baltiapps.migrate.domain.backup.sources.DBWriter
import baltiapps.migrate.domain.backup.sources.PlatformDataSource
import baltiapps.migrate.domain.backup.sources.PlatformFileSystemSource
import baltiapps.migrate.domain.backup.sources.TextWriter
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

class DataRepository(
    private val contactsSource: PlatformDataSource<ContactData>,
    private val callLogSource: PlatformDataSource<CallLogData>,
    private val smsSource: PlatformDataSource<SmsData>,
    private val textWriter: TextWriter,
    private val callLogDBWriter: DBWriter<CallLogData>,
    private val smsDBWriter: DBWriter<SmsData>,
    private val fileSystemSource: PlatformFileSystemSource,
) : PlatformDataRepository() {

    override val contactsDataItems: MutableList<ContactData> = mutableListOf()
    override val callLogDataItems: MutableList<CallLogData> = mutableListOf()
    override val smsDataItems: MutableList<SmsData> = mutableListOf()

    override val stagedContacts: MutableList<ContactData> = mutableListOf()
    override val stagedCallLogs: MutableList<CallLogData> = mutableListOf()
    override val stagedSms: MutableList<SmsData> = mutableListOf()

    override suspend fun readContactsFromDevice(): Flow<Progress> {
        return collectData(contactsSource::getData, contactsDataItems)
    }

    override suspend fun readCallLogsFromDevice(): Flow<Progress> {
        return collectData(callLogSource::getData, callLogDataItems)
    }

    override suspend fun readSmsFromDevice(): Flow<Progress> {
        return collectData(smsSource::getData, smsDataItems)
    }

    override fun setStagedContacts(ids: List<String>) {
        setStagedItems(ids, contactsDataItems, stagedContacts)
    }

    override fun setStagedCallLogs(ids: List<String>) {
        setStagedItems(ids, callLogDataItems, stagedCallLogs)
    }

    override fun setStagedSms(ids: List<String>) {
        setStagedItems(ids, smsDataItems, stagedSms)
    }

    private suspend fun <T : DataItem<*>> FlowCollector<Progress>.tryPerformWrite(
        items: List<T>,
        progressType: Progress.ProgressType,
        writeBlock: (item: T) -> Unit,
    ) {
        items.forEachIndexed { index, item ->
            val progress = Progress(
                progressType = progressType,
                percentage = getPercentage(index + 1, items.size),
                logs = "(${index + 1}/${items.size}) ${item.logInfo}"
            )
            emit(
                try {
                    writeBlock(item)
                    progress
                } catch (e: Exception) {
                    progress.copy(
                        logs = "${item.logInfo} - ${e.message}",
                        isFailure = true
                    )
                }
            )
        }
    }

    override fun backupContacts(backupRoot: String): Flow<Progress> {
        return flow {
            if (stagedContacts.isEmpty()) return@flow
            fileSystemSource.writeText(
                directory = backupRoot,
                fileName = BACKUP_FILE_NAME_CONTACTS,
                append = false,
                textWriter = textWriter,
            ) { writer ->
                tryPerformWrite(
                    items = stagedContacts,
                    progressType = Progress.ProgressType.CONTACTS_BACKUP,
                ) { item ->
//                    if (item._id.toInt() % 2 == 0) {
//                        throw Exception("Haha ${item.logInfo}")
//                    }
                    writer.writeLine(item.vcfContent)
                }
            }
        }.flowOn(IO)
    }

    override fun backupCalls(backupRoot: String): Flow<Progress> {
        return flow {
            if (stagedCallLogs.isEmpty()) return@flow
            fileSystemSource.writeDB(
                directory = backupRoot,
                fileName = BACKUP_FILE_NAME_CALL_LOGS,
                dbWriter = callLogDBWriter,
            ) { dbWriter ->
                tryPerformWrite(
                    items = stagedCallLogs,
                    progressType = Progress.ProgressType.CALL_LOG_BACKUP,
                ) { item ->
                    dbWriter.writeRow(item)
                }
            }
        }.flowOn(IO)
    }

    override fun backupSms(backupRoot: String): Flow<Progress> {
        return flow {
            if (stagedSms.isEmpty()) return@flow
            fileSystemSource.writeDB(
                directory = backupRoot,
                fileName = BACKUP_FILE_NAME_SMS,
                dbWriter = smsDBWriter,
            ) { dbWriter ->
                tryPerformWrite(
                    items = stagedSms,
                    progressType = Progress.ProgressType.SMS_BACKUP,
                ) { item ->
                    dbWriter.writeRow(item)
                }
            }
        }.flowOn(IO)
    }
}