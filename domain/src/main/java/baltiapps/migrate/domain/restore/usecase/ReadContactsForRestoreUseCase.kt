package baltiapps.migrate.domain.restore.usecase

import baltiapps.migrate.domain.common.clearAndAddAll
import baltiapps.migrate.domain.common.model.ContactListItem
import baltiapps.migrate.domain.common.model.DataItem
import baltiapps.migrate.domain.common.model.Progress
import baltiapps.migrate.domain.restore.repository.RestoreDataRepository
import baltiapps.migrate.domain.restore.sources.RestoreReader
import baltiapps.migrate.domain.restore.sources.restoreReaderRunner
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

class ReadContactsForRestoreUseCase(
    private val contactsRestoreReader: RestoreReader<DataItem<ContactListItem>>,
    private val restoreDataRepository: RestoreDataRepository,
) {
    operator fun invoke(): Flow<Progress> {
        return restoreDataRepository.run {
            val contactsFile = this.getContactBackupFile() ?: return emptyFlow()

            restoreReaderRunner(
                file = contactsFile,
                restoreReader = contactsRestoreReader,
                onItemsRead = { dataItems ->
                    this.contactsDataItems.clearAndAddAll(dataItems)
                }
            )
        }
    }
}