package baltiapps.migrate.domain.backup.usecase

import baltiapps.migrate.domain.backup.repository.BackupDataRepository
import baltiapps.migrate.domain.backup.sources.BackupEngine
import baltiapps.migrate.domain.backup.sources.backupEngineRunner
import baltiapps.migrate.domain.common.model.ContactListItem
import baltiapps.migrate.domain.common.model.DataItem
import baltiapps.migrate.domain.common.model.GenericFile
import baltiapps.migrate.domain.common.model.Progress
import kotlinx.coroutines.flow.Flow

class BackupContactsUseCase(
    private val contactsDBWriter: BackupEngine<DataItem<ContactListItem>>,
    private val dataRepository: BackupDataRepository,
) {
    operator fun invoke(
        file: GenericFile,
    ): Flow<Progress> {
        return backupEngineRunner(
            backupEngine = contactsDBWriter,
            location = file,
            items = dataRepository.stagedContacts
        )
    }
}