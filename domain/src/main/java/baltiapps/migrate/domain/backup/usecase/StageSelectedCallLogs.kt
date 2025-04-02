package baltiapps.migrate.domain.backup.usecase

import baltiapps.migrate.domain.backup.clearAndAddAll
import baltiapps.migrate.domain.backup.model.CallLogListItem
import baltiapps.migrate.domain.backup.repository.DataRepository

class StageSelectedCallLogs(
    private val dataRepository: DataRepository,
) {
    operator fun invoke(
        allListItems: List<CallLogListItem>
    ) {
        val selectedIds = allListItems.filter { it.isChecked }.map { it._id }
        val selectedDataItems = dataRepository.callLogDataItems.filter { it._id in selectedIds }
        dataRepository.stagedCallLogs.clearAndAddAll(selectedDataItems)
    }
}