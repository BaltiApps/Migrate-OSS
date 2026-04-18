package baltiapps.migrate.domain.backup.usecase

import baltiapps.migrate.domain.backup.repository.BackupDataRepository
import baltiapps.migrate.domain.backup.sources.BackupEngine
import baltiapps.migrate.domain.backup.sources.backupEngineRunner
import baltiapps.migrate.domain.common.model.AppListItem
import baltiapps.migrate.domain.common.model.DataItem
import baltiapps.migrate.domain.common.model.Progress
import kotlinx.coroutines.flow.Flow

class BackupExternalDataUseCase(
    private val externalDataBackupEngine: BackupEngine<DataItem<AppListItem>>,
    private val dataRepository: BackupDataRepository,
) {
    operator fun invoke(
        destination: String,
    ): Flow<Progress> {
        return backupEngineRunner(
            backupEngine = externalDataBackupEngine,
            location = destination,
            items = dataRepository.stagedApps
        )
    }
}
