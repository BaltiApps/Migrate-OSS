package baltiapps.migrate.domain.restore.usecase

import baltiapps.migrate.domain.backup.repository.BackupDataRepository
import baltiapps.migrate.domain.backup.sources.DataSource
import baltiapps.migrate.domain.common.clearAndAddAll
import baltiapps.migrate.domain.common.model.AppSizeInfo
import baltiapps.migrate.domain.common.model.Progress
import kotlinx.coroutines.flow.Flow

class CalculateStagedAppsSizesUseCase(
    private val backupDataRepository: BackupDataRepository,
    private val appSizeReader: DataSource<AppSizeInfo>,
) {
    suspend operator fun invoke(): Flow<Progress> {
        return appSizeReader.getData { appSizeInfos ->
            backupDataRepository.stagedAppSizes.clearAndAddAll(appSizeInfos)
        }
    }
}