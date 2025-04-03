package baltiapps.migrate.domain.backup.usecase

import baltiapps.migrate.domain.backup.clearAndAddAll
import baltiapps.migrate.domain.backup.model.ContactListItem
import baltiapps.migrate.domain.common.model.DataItem
import baltiapps.migrate.domain.backup.model.Progress
import baltiapps.migrate.domain.backup.repository.DataRepository
import baltiapps.migrate.domain.backup.sources.DataSource
import kotlinx.coroutines.flow.Flow

class ReadContactsUseCase(
    private val contactsSource: DataSource<DataItem<ContactListItem>>,
    private val dataRepository: DataRepository,
) {
    suspend operator fun invoke(): Flow<Progress> {
        return contactsSource.getData {
            dataRepository.contactsDataItems.clearAndAddAll(it)
        }
    }
}