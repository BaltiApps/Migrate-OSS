package baltiapps.migrate.domain.backup.usecase

import baltiapps.migrate.domain.backup.repository.BackupDataRepository
import baltiapps.migrate.domain.backup.sources.DataSource
import baltiapps.migrate.domain.common.clearAndAddAll
import baltiapps.migrate.domain.common.model.AppListItem
import baltiapps.migrate.domain.common.model.DataItem
import baltiapps.migrate.domain.common.model.Progress
import kotlinx.coroutines.flow.Flow

class ReadAppListForBackupUseCase(
    private val appListSource: DataSource<DataItem<AppListItem>>,
    private val backupDataRepository: BackupDataRepository,
) {
    suspend operator fun invoke(): Flow<Progress> {
        return appListSource.getData {
            backupDataRepository.appDataItems.clearAndAddAll(it)
        }
    }
}