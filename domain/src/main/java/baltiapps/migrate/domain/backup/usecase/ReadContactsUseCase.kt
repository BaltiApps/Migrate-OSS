package baltiapps.migrate.domain.backup.usecase

import baltiapps.migrate.domain.backup.model.ContactListItem
import baltiapps.migrate.domain.backup.model.Progress
import baltiapps.migrate.domain.backup.repository.DataRepository
import baltiapps.migrate.domain.backup.toListItems
import kotlinx.coroutines.flow.Flow

class ReadContactsUseCase(
    private val dataRepository: DataRepository,
) {
    suspend fun read(): Flow<Progress> {
        return dataRepository.readContactsFromDevice()
    }

    fun getReadContacts(): List<ContactListItem> {
        return dataRepository.contactsDataItems.toListItems().sortedBy { it.displayName }
    }
}