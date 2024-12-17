package baltiapps.migrate.domain.backup.usecase

import baltiapps.migrate.domain.backup.model.CallLogListItem
import baltiapps.migrate.domain.backup.model.Progress
import baltiapps.migrate.domain.backup.repository.PlatformDataRepository
import baltiapps.migrate.domain.backup.toListItems
import kotlinx.coroutines.flow.Flow

class ReadCallLogUseCase(
    private val platformDataRepository: PlatformDataRepository,
) {
    suspend fun read(): Flow<Progress> {
        return platformDataRepository.readCallLogsFromDevice()
    }

    fun getReadCallLogs(): List<CallLogListItem> {
        return platformDataRepository.callLogDataItems.toListItems()
            .sortedByDescending { it.creationDate.dateInLong }
    }
}