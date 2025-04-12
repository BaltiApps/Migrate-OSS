package baltiapps.migrate.domain.backup.usecase

import baltiapps.migrate.domain.common.clearAndAddAll
import baltiapps.migrate.domain.common.model.ContactListItem
import baltiapps.migrate.domain.common.model.DataItem
import baltiapps.migrate.domain.common.model.Progress
import baltiapps.migrate.domain.backup.repository.BackupDataRepository
import baltiapps.migrate.domain.backup.sources.DataSource
import kotlinx.coroutines.flow.Flow

class ReadContactsForBackupUseCase(
    private val contactsSource: DataSource<DataItem<ContactListItem>>,
    private val backupDataRepository: BackupDataRepository,
) {
    suspend operator fun invoke(): Flow<Progress> {
        return contactsSource.getData {
            backupDataRepository.contactsDataItems.clearAndAddAll(it)
        }
    }
}