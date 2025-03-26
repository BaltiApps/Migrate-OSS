package baltiapps.migrate.domain.backup.usecase

import baltiapps.migrate.domain.backup.model.CallLogListItem
import baltiapps.migrate.domain.backup.repository.DataRepository

class StageSelectedCallLogs(
    private val dataRepository: DataRepository,
) {
    operator fun invoke(
        allListItems: List<CallLogListItem>
    ) {
        val selectedIds = allListItems.filter { it.isChecked }.map { it._id }
        dataRepository.setStagedCallLogs(selectedIds)
    }
}