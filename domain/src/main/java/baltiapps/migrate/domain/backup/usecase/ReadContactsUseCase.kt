package baltiapps.migrate.domain.backup.usecase

import baltiapps.migrate.domain.backup.model.ContactListItem
import baltiapps.migrate.domain.backup.model.Progress
import baltiapps.migrate.domain.backup.repository.PlatformDataRepository
import baltiapps.migrate.domain.backup.toListItems
import kotlinx.coroutines.flow.Flow

class ReadContactsUseCase(
    private val platformDataRepository: PlatformDataRepository,
) {
    suspend fun read(): Flow<Progress> {
        return platformDataRepository.readContactsFromDevice()
    }

    fun getReadContacts(): List<ContactListItem> {
        return platformDataRepository.contactsDataItems.toListItems().sortedBy { it.displayName }
    }
}