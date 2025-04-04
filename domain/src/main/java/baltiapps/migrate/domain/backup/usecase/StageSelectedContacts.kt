package baltiapps.migrate.domain.backup.usecase

import baltiapps.migrate.domain.common.clearAndAddAll
import baltiapps.migrate.domain.common.model.ContactListItem
import baltiapps.migrate.domain.backup.repository.BackupDataRepository

class StageSelectedContacts(
    private val backupDataRepository: BackupDataRepository,
) {
    operator fun invoke(
        allListItems: List<ContactListItem>
    ) {
        val selectedIds = allListItems.filter { it.isChecked }.map { it._id }
        val selectedDataItems = backupDataRepository.contactsDataItems.filter { it._id in selectedIds }
        backupDataRepository.stagedContacts.clearAndAddAll(selectedDataItems)
    }
}