package baltiapps.migrate.domain.backup.usecase

import baltiapps.migrate.domain.backup.model.CallLogListItem
import baltiapps.migrate.domain.backup.repository.PlatformDataRepository

class StageSelectedCallLogs(
    private val platformDataRepository: PlatformDataRepository,
) {
    operator fun invoke(
        allListItems: List<CallLogListItem>
    ) {
        val selectedIds = allListItems.filter { it.isChecked }.map { it._id }
        platformDataRepository.setStagedCallLogs(selectedIds)
    }
}