package baltiapps.migrate.domain.restore.usecase

import baltiapps.migrate.domain.common.model.CallLogListItem
import baltiapps.migrate.domain.common.model.DataItem
import baltiapps.migrate.domain.common.model.Progress
import baltiapps.migrate.domain.restore.repository.RestoreDataRepository
import baltiapps.migrate.domain.restore.sources.DataRestore
import kotlinx.coroutines.flow.Flow

class RestoreCallLogUseCase(
    private val dataRestore: DataRestore<DataItem<CallLogListItem>>,
    private val restoreDataRepository: RestoreDataRepository,
) {
    operator fun invoke(): Flow<Progress> {
        return dataRestore.restoreDataItems(restoreDataRepository.callLogDataItems)
    }
}