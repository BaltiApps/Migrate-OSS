package baltiapps.migrate.domain.restore.usecase

import baltiapps.migrate.domain.common.clearAndAddAll
import baltiapps.migrate.domain.common.model.CallLogListItem
import baltiapps.migrate.domain.common.model.DataItem
import baltiapps.migrate.domain.common.model.Progress
import baltiapps.migrate.domain.common.sources.fileSystem.DBReader
import baltiapps.migrate.domain.common.sources.fileSystem.FileSystemSource
import baltiapps.migrate.domain.restore.repository.RestoreDataRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

class ReadCallLogForRestoreUseCase(
    private val fileSystemSource: FileSystemSource,
    private val callLogDbReader: DBReader<DataItem<CallLogListItem>>,
    private val restoreDataRepository: RestoreDataRepository,
) {
    operator fun invoke(): Flow<Progress> {
        return restoreDataRepository.run {
            val callLogFile = this.getCallLogBackupFile() ?: return emptyFlow()
            fileSystemSource.readDB(callLogFile, callLogDbReader) { reader ->
                reader.readRows { result ->
                    this.callLogDataItems.clearAndAddAll(result)
                }
            }
        }
    }
}