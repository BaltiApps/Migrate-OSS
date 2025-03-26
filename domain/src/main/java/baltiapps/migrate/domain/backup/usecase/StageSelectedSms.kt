package baltiapps.migrate.domain.backup.usecase

import baltiapps.migrate.domain.backup.model.SmsListItem
import baltiapps.migrate.domain.backup.repository.DataRepository

class StageSelectedSms(
    private val dataRepository: DataRepository,
) {
    operator fun invoke(
        allListItems: List<SmsListItem>
    ) {
        val selectedIds = allListItems.filter { it.isChecked }.map { it._id }
        dataRepository.setStagedSms(selectedIds)
    }
}