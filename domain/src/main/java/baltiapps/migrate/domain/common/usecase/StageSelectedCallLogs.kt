package baltiapps.migrate.domain.common.usecase

import baltiapps.migrate.domain.common.clearAndAddAll
import baltiapps.migrate.domain.common.model.CallLogListItem
import baltiapps.migrate.domain.common.repository.DataRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class StageSelectedCallLogs {
    suspend operator fun invoke(
        allListItems: List<CallLogListItem>,
        dataRepository: DataRepository,
    ) {
        withContext(Dispatchers.IO) {
            val selectedIds = allListItems.filter { it.isChecked }.map { it._id }
            val selectedDataItems = dataRepository.callLogDataItems.filter { it._id in selectedIds }
            dataRepository.stagedCallLogs.clearAndAddAll(selectedDataItems)
        }
    }
}