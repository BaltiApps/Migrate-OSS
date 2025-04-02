package baltiapps.migrate.domain.backup.usecase

import baltiapps.migrate.domain.backup.clearAndAddAll
import baltiapps.migrate.domain.backup.model.ContactListItem
import baltiapps.migrate.domain.backup.repository.DataRepository

class StageSelectedContacts(
    private val dataRepository: DataRepository,
) {
    operator fun invoke(
        allListItems: List<ContactListItem>
    ) {
        val selectedIds = allListItems.filter { it.isChecked }.map { it._id }
        val selectedDataItems = dataRepository.contactsDataItems.filter { it._id in selectedIds }
        dataRepository.stagedContacts.clearAndAddAll(selectedDataItems)
    }
}