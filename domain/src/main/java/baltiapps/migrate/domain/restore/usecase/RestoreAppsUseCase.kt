package baltiapps.migrate.domain.restore.usecase

import baltiapps.migrate.domain.common.model.AppListItem
import baltiapps.migrate.domain.common.model.DataItem
import baltiapps.migrate.domain.common.model.GenericFile
import baltiapps.migrate.domain.common.model.Progress
import baltiapps.migrate.domain.common.sources.fileSystem.FileSystemSource
import baltiapps.migrate.domain.common.sources.fileSystem.GenericReader
import baltiapps.migrate.domain.restore.repository.RestoreDataRepository
import kotlinx.coroutines.flow.Flow

class RestoreAppsUseCase(
    private val fileSystemSource: FileSystemSource,
    private val appRestoreEngine: GenericReader<List<DataItem<AppListItem>>>,
    private val dataRepository: RestoreDataRepository,
) {
    operator fun invoke(
        source: GenericFile,
    ): Flow<Progress> {
        return fileSystemSource.read(
            file = source,
            reader = appRestoreEngine,
            readerBlock = {
                it.read(dataRepository.stagedApps)
            }
        )
    }
}
