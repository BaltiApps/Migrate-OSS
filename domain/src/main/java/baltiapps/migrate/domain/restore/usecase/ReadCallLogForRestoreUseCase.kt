package baltiapps.migrate.domain.restore.usecase

import baltiapps.migrate.domain.common.clearAndAddAll
import baltiapps.migrate.domain.common.model.CallLogListItem
import baltiapps.migrate.domain.common.model.DataItem
import baltiapps.migrate.domain.common.model.Progress
import baltiapps.migrate.domain.restore.repository.RestoreDataRepository
import baltiapps.migrate.domain.restore.sources.RestoreReader
import baltiapps.migrate.domain.restore.sources.restoreReaderRunner
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

class ReadCallLogForRestoreUseCase(
    private val callLogRestoreReader: RestoreReader<DataItem<CallLogListItem>>,
    private val restoreDataRepository: RestoreDataRepository,
) {
    operator fun invoke(): Flow<Progress> {
        return restoreDataRepository.run {
            val callLogFile = this.getCallLogBackupFile() ?: return emptyFlow()

            restoreReaderRunner(
                file = callLogFile,
                restoreReader = callLogRestoreReader,
                onItemsRead = { dataItems ->
                    this.callLogDataItems.clearAndAddAll(dataItems)
                }
            )
        }
    }
}