package baltiapps.migrate.domain.backup.usecase

import baltiapps.migrate.domain.backup.repository.BackupDataRepository
import baltiapps.migrate.domain.backup.sources.DataBackup
import baltiapps.migrate.domain.backup.sources.dataBackupRunner
import baltiapps.migrate.domain.common.model.ContactListItem
import baltiapps.migrate.domain.common.model.DataItem
import baltiapps.migrate.domain.common.model.GenericFile
import baltiapps.migrate.domain.common.model.Progress
import kotlinx.coroutines.flow.Flow

class BackupContactsUseCase(
    private val contactsDBWriter: DataBackup<DataItem<ContactListItem>>,
    private val dataRepository: BackupDataRepository,
) {
    operator fun invoke(
        file: GenericFile,
    ): Flow<Progress> {
        return dataBackupRunner(
            backupEngine = contactsDBWriter,
            location = file,
            items = dataRepository.stagedContacts
        )
    }
}