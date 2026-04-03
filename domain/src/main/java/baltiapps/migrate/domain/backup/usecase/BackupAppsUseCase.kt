package baltiapps.migrate.domain.backup.usecase

import baltiapps.migrate.domain.backup.repository.BackupDataRepository
import baltiapps.migrate.domain.backup.sources.DataBackup
import baltiapps.migrate.domain.backup.sources.dataBackupRunner
import baltiapps.migrate.domain.common.model.AppListItem
import baltiapps.migrate.domain.common.model.DataItem
import baltiapps.migrate.domain.common.model.Progress
import kotlinx.coroutines.flow.Flow

class BackupAppsUseCase(
    private val appBackupEngine: DataBackup<DataItem<AppListItem>>,
    private val dataRepository: BackupDataRepository,
) {
    operator fun invoke(
        destination: String,
    ): Flow<Progress> {
        return dataBackupRunner(
            backupEngine = appBackupEngine,
            location = destination,
            items = dataRepository.stagedApps
        )
    }
}