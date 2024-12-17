package baltiapps.migrate.domain.backup.usecase

import baltiapps.migrate.domain.backup.model.SmsListItem
import baltiapps.migrate.domain.backup.repository.PlatformDataRepository

class StageSelectedSms(
    private val platformDataRepository: PlatformDataRepository,
) {
    operator fun invoke(
        allListItems: List<SmsListItem>
    ) {
        val selectedIds = allListItems.filter { it.isChecked }.map { it._id }
        platformDataRepository.setStagedSms(selectedIds)
    }
}