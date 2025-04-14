package baltiapps.migrate.domain.restore.usecase

import baltiapps.migrate.domain.common.clearAndAddAll
import baltiapps.migrate.domain.common.model.ContactListItem
import baltiapps.migrate.domain.common.model.DataItem
import baltiapps.migrate.domain.common.model.Progress
import baltiapps.migrate.domain.common.sources.fileSystem.DBReader
import baltiapps.migrate.domain.common.sources.fileSystem.FileSystemSource
import baltiapps.migrate.domain.restore.repository.RestoreDataRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

class ReadContactsForRestoreUseCase(
    private val fileSystemSource: FileSystemSource,
    private val contactsDbReader: DBReader<DataItem<ContactListItem>>,
    private val restoreDataRepository: RestoreDataRepository,
) {
    operator fun invoke(): Flow<Progress> {
        return restoreDataRepository.run {
            val contactsFile = this.getContactBackupFile() ?: return emptyFlow()
            fileSystemSource.readDB(contactsFile, contactsDbReader) { reader ->
                reader.readRows { result ->
                    this.contactsDataItems.clearAndAddAll(result)
                }
            }
        }
    }
}