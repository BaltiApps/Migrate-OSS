package baltiapps.migrate.domain.backup.usecase

import baltiapps.migrate.domain.backup.model.ContactListItem
import baltiapps.migrate.domain.backup.repository.DataRepository

class StageSelectedContacts(
    private val dataRepository: DataRepository,
) {
    operator fun invoke(
        allListItems: List<ContactListItem>
    ) {
        val selectedIds = allListItems.filter { it.isChecked }.map { it._id }
        dataRepository.setStagedContacts(selectedIds)
    }
}