package baltiapps.migrate.domain.backup.usecase

import baltiapps.migrate.domain.backup.clearAndAddAll
import baltiapps.migrate.domain.backup.model.CallLogListItem
import baltiapps.migrate.domain.common.model.DataItem
import baltiapps.migrate.domain.backup.model.Progress
import baltiapps.migrate.domain.backup.repository.DataRepository
import baltiapps.migrate.domain.backup.sources.DataSource
import kotlinx.coroutines.flow.Flow

class ReadCallLogUseCase(
    private val callLogSource: DataSource<DataItem<CallLogListItem>>,
    private val dataRepository: DataRepository,
) {
    suspend fun invoke(): Flow<Progress> {
        return callLogSource.getData {
            dataRepository.callLogDataItems.clearAndAddAll(it)
        }
    }
}