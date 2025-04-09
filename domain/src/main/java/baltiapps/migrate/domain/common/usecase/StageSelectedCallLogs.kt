package baltiapps.migrate.domain.common.usecase

import baltiapps.migrate.domain.common.clearAndAddAll
import baltiapps.migrate.domain.common.model.CallLogListItem
import baltiapps.migrate.domain.common.repository.DataRepository

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