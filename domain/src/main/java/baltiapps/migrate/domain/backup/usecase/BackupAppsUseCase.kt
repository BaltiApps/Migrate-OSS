package baltiapps.migrate.domain.backup.usecase

import baltiapps.migrate.domain.backup.repository.BackupDataRepository
import baltiapps.migrate.domain.common.model.AppListItem
import baltiapps.migrate.domain.common.model.DataItem
import baltiapps.migrate.domain.common.model.GenericFile
import baltiapps.migrate.domain.common.model.Progress
import baltiapps.migrate.domain.common.sources.fileSystem.FileSystemSource
import baltiapps.migrate.domain.common.sources.fileSystem.GenericWriter
import kotlinx.coroutines.flow.Flow

class BackupAppsUseCase(
    private val fileSystemSource: FileSystemSource,
    private val appBackupEngine: GenericWriter<List<DataItem<AppListItem>>>,
    private val dataRepository: BackupDataRepository,
) {
    operator fun invoke(
        destination: GenericFile,
    ): Flow<Progress> {
        return fileSystemSource.write(
            file = destination,
            writer = appBackupEngine,
            writerBlock = {
                it.write(dataRepository.stagedApps)
            }
        )
    }
}