package baltiapps.migrate.domain.backup.usecase

import baltiapps.migrate.domain.common.clearAndAddAll
import baltiapps.migrate.domain.common.model.CallLogListItem
import baltiapps.migrate.domain.common.model.DataItem
import baltiapps.migrate.domain.common.model.Progress
import baltiapps.migrate.domain.backup.repository.BackupDataRepository
import baltiapps.migrate.domain.backup.sources.DataSource
import kotlinx.coroutines.flow.Flow

class ReadCallLogUseCase(
    private val callLogSource: DataSource<DataItem<CallLogListItem>>,
    private val backupDataRepository: BackupDataRepository,
) {
    suspend fun invoke(): Flow<Progress> {
        return callLogSource.getData {
            backupDataRepository.callLogDataItems.clearAndAddAll(it)
        }
    }
}