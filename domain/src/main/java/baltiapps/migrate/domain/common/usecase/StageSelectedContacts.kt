package baltiapps.migrate.domain.common.usecase

import baltiapps.migrate.domain.backup.repository.BackupDataRepository
import baltiapps.migrate.domain.common.clearAndAddAll
import baltiapps.migrate.domain.common.model.ContactListItem

class StageSelectedContacts {
    operator fun invoke(
        allListItems: List<ContactListItem>,
        dataRepository: BackupDataRepository,
    ) {
        val selectedIds = allListItems.filter { it.isChecked }.map { it._id }
        val selectedDataItems = dataRepository.contactsDataItems.filter { it._id in selectedIds }
        dataRepository.stagedContacts.clearAndAddAll(selectedDataItems)
    }
}