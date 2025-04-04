package baltiapps.migrate.domain.backup.usecase

import baltiapps.migrate.domain.backup.clearAndAddAll
import baltiapps.migrate.domain.backup.model.CallLogListItem
import baltiapps.migrate.domain.backup.repository.BackupDataRepository

class StageSelectedCallLogs(
    private val backupDataRepository: BackupDataRepository,
) {
    operator fun invoke(
        allListItems: List<CallLogListItem>
    ) {
        val selectedIds = allListItems.filter { it.isChecked }.map { it._id }
        val selectedDataItems = backupDataRepository.callLogDataItems.filter { it._id in selectedIds }
        backupDataRepository.stagedCallLogs.clearAndAddAll(selectedDataItems)
    }
}