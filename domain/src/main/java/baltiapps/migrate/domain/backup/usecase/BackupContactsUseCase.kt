package baltiapps.migrate.domain.backup.usecase

import baltiapps.migrate.domain.BACKUP_FILE_NAME_CONTACTS
import baltiapps.migrate.domain.backup.model.ContactListItem
import baltiapps.migrate.domain.backup.model.DataItem
import baltiapps.migrate.domain.backup.model.Progress
import baltiapps.migrate.domain.backup.sources.FileSystemSource
import baltiapps.migrate.domain.backup.sources.TextWriter
import baltiapps.migrate.domain.backup.tryPerformWrite
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

class BackupContactsUseCase(
    private val fileSystemSource: FileSystemSource,
    private val contactsWriter: TextWriter<DataItem<ContactListItem>>,
) {
    operator fun invoke(
        backupRoot: String,
        stagedContacts: List<DataItem<ContactListItem>>,
    ): Flow<Progress> {
        return flow {
            if (stagedContacts.isEmpty()) return@flow
            fileSystemSource.writeText(
                directory = backupRoot,
                fileName = BACKUP_FILE_NAME_CONTACTS,
                append = false,
                textWriter = contactsWriter,
            ) { writer ->
                tryPerformWrite(
                    items = stagedContacts,
                    progressType = Progress.ProgressType.CONTACTS_BACKUP,
                ) { item ->
                    writer.writeLine(item)
                }
            }
        }.flowOn(IO)
    }
}