package baltiapps.migrate.domain.backup.usecase

import baltiapps.migrate.domain.backup.repository.BackupDataRepository
import baltiapps.migrate.domain.common.model.CallLogListItem
import baltiapps.migrate.domain.common.model.DataItem
import baltiapps.migrate.domain.common.model.Directory
import baltiapps.migrate.domain.common.model.GenericFile
import baltiapps.migrate.domain.common.model.Progress
import baltiapps.migrate.domain.common.sources.fileSystem.REWRITE_DBWriter
import baltiapps.migrate.domain.common.sources.fileSystem.FileSystemSource
import kotlinx.coroutines.flow.Flow

class BackupCallLogUseCase(
    private val fileSystemSource: FileSystemSource,
    private val callLogDBWriter: REWRITE_DBWriter<DataItem<CallLogListItem>>,
    private val dataRepository: BackupDataRepository,
) {
    operator fun invoke(
        directory: Directory,
        file: GenericFile,
    ): Flow<Progress> {
        return fileSystemSource.writeDB(
            directory = directory,
            file = file,
            dbWriter = callLogDBWriter,
            writerBlock = {
                it.writeRows(dataRepository.stagedCallLogs)
            }
        )
    }
}