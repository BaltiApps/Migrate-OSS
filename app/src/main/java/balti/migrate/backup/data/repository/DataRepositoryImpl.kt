package balti.migrate.backup.data.repository

import balti.migrate.backup.data.model.ContactData
import baltiapps.migrate.domain.BACKUP_FILE_NAME_CONTACTS
import baltiapps.migrate.domain.backup.getPercentage
import baltiapps.migrate.domain.backup.model.DataItem
import baltiapps.migrate.domain.backup.model.Progress
import baltiapps.migrate.domain.backup.repository.DataRepository
import baltiapps.migrate.domain.backup.sources.DataSource
import baltiapps.migrate.domain.backup.sources.FileSystemSource
import baltiapps.migrate.domain.backup.sources.TextWriter
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

class DataRepositoryImpl(
    private val contactsSource: DataSource<ContactData>,
    private val textWriter: TextWriter<String>,
    private val fileSystemSource: FileSystemSource,
) : DataRepository() {

    override val contactsDataItems: MutableList<ContactData> = mutableListOf()

    override val stagedContacts: MutableList<ContactData> = mutableListOf()

    override suspend fun readContactsFromDevice(): Flow<Progress> {
        return collectData(contactsSource::getData, contactsDataItems)
    }

    override fun setStagedContacts(ids: List<String>) {
        setStagedItems(ids, contactsDataItems, stagedContacts)
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
                    writer.writeLine(item.vcfContent)
                }
            }
        }.flowOn(IO)
    }
}