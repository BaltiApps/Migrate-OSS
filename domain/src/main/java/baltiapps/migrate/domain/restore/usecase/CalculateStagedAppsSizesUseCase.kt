package baltiapps.migrate.domain.restore.usecase

import baltiapps.migrate.domain.backup.repository.BackupDataRepository
import baltiapps.migrate.domain.common.clearAndAddAll
import baltiapps.migrate.domain.common.model.AppListItem
import baltiapps.migrate.domain.common.model.AppSizeInfo
import baltiapps.migrate.domain.common.model.DataItem
import baltiapps.migrate.domain.common.model.Progress
import baltiapps.migrate.domain.common.sources.fileSystem.GenericReader
import kotlinx.coroutines.flow.Flow

class CalculateStagedAppsSizesUseCase(
    private val backupDataRepository: BackupDataRepository,
    private val appSizeReader: GenericReader<List<DataItem<AppListItem>>, List<AppSizeInfo>>,
) {
    operator fun invoke(): Flow<Progress> {
        return appSizeReader.read(
            data = backupDataRepository.stagedApps,
            onComplete = {
                backupDataRepository.stagedAppSizes.clearAndAddAll(it)
            }
        )
    }
}