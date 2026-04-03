package baltiapps.migrate.domain.backup.usecase

import baltiapps.migrate.domain.backup.repository.BackupDataRepository
import baltiapps.migrate.domain.backup.sources.DataBackup
import baltiapps.migrate.domain.backup.sources.dataBackupRunner
import baltiapps.migrate.domain.common.model.CallLogListItem
import baltiapps.migrate.domain.common.model.DataItem
import baltiapps.migrate.domain.common.model.GenericFile
import baltiapps.migrate.domain.common.model.Progress
import kotlinx.coroutines.flow.Flow

class BackupCallLogUseCase(
    private val callLogDBWriter: DataBackup<DataItem<CallLogListItem>>,
    private val dataRepository: BackupDataRepository,
) {
    operator fun invoke(
        file: GenericFile,
    ): Flow<Progress> {
        return dataBackupRunner(
            backupEngine = callLogDBWriter,
            location = file,
            items = dataRepository.stagedCallLogs
        )
    }
}