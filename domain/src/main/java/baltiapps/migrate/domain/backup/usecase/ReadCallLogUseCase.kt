package baltiapps.migrate.domain.backup.usecase

import baltiapps.migrate.domain.backup.model.CallLogListItem
import baltiapps.migrate.domain.backup.model.Progress
import baltiapps.migrate.domain.backup.repository.DataRepository
import baltiapps.migrate.domain.backup.toListItems
import kotlinx.coroutines.flow.Flow

class ReadCallLogUseCase(
    private val dataRepository: DataRepository,
) {
    suspend fun read(): Flow<Progress> {
        return dataRepository.readCallLogsFromDevice()
    }

    fun getReadCallLogs(): List<CallLogListItem> {
        return dataRepository.callLogDataItems.toListItems()
            .sortedByDescending { it.creationDate.dateInLong }
    }
}