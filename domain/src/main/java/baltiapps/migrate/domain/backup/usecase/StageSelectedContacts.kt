package baltiapps.migrate.domain.backup.usecase

import baltiapps.migrate.domain.backup.model.ContactListItem
import baltiapps.migrate.domain.backup.repository.PlatformDataRepository

class StageSelectedContacts(
    private val platformDataRepository: PlatformDataRepository,
) {
    operator fun invoke(
        allListItems: List<ContactListItem>
    ) {
        val selectedIds = allListItems.filter { it.isChecked }.map { it._id }
        platformDataRepository.setStagedContacts(selectedIds)
    }
}