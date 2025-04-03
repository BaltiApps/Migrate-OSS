package baltiapps.migrate.domain.backup.usecase

import baltiapps.migrate.domain.BACKUP_FILE_NAME_CALL_LOGS
import baltiapps.migrate.domain.backup.model.CallLogListItem
import baltiapps.migrate.domain.common.model.DataItem
import baltiapps.migrate.domain.common.model.Progress
import baltiapps.migrate.domain.common.sources.fileSystem.DBWriter
import baltiapps.migrate.domain.common.sources.fileSystem.FileSystemSource
import baltiapps.migrate.domain.backup.tryPerformWrite
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

class BackupCallLogUseCase(
    private val fileSystemSource: FileSystemSource,
    private val callLogDBWriter: DBWriter<DataItem<CallLogListItem>>,
) {
    operator fun invoke(
        backupRoot: String,
        stagedCallLogs: List<DataItem<CallLogListItem>>
    ): Flow<Progress> {
        return flow {
            if (stagedCallLogs.isEmpty()) return@flow
            fileSystemSource.writeDB(
                directory = backupRoot,
                fileName = BACKUP_FILE_NAME_CALL_LOGS,
                dbWriter = callLogDBWriter,
            ) { dbWriter ->
                tryPerformWrite(
                    items = stagedCallLogs,
                    progressType = Progress.ProgressType.CALL_LOG_BACKUP,
                ) { item ->
                    dbWriter.writeRow(item)
                }
            }
        }.flowOn(IO)
    }
}