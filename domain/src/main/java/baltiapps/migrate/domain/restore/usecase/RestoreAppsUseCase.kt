package baltiapps.migrate.domain.restore.usecase

import baltiapps.migrate.domain.common.model.AppListItem
import baltiapps.migrate.domain.common.model.DataItem
import baltiapps.migrate.domain.common.model.GenericFile
import baltiapps.migrate.domain.common.model.Progress
import baltiapps.migrate.domain.common.sources.fileSystem.FileSystemSource
import baltiapps.migrate.domain.restore.repository.RestoreDataRepository
import baltiapps.migrate.domain.restore.sources.RestoreEngine
import baltiapps.migrate.domain.restore.sources.restoreEngineRunner
import kotlinx.coroutines.flow.Flow

class RestoreAppsUseCase(
    private val fileSystemSource: FileSystemSource,
    private val appRestoreEngine: RestoreEngine<DataItem<AppListItem>>,
    private val dataRepository: RestoreDataRepository,
) {
    operator fun invoke(
        source: GenericFile,
    ): Flow<Progress> {

        val locationPath = fileSystemSource.getLocalFilePath(source)

        if (locationPath.isNullOrBlank()) {
            throw Exception("Location is null or blank")
        }

        return restoreEngineRunner(
            restoreEngine = appRestoreEngine,
            location = locationPath,
            items = dataRepository.stagedApps,
        )
    }
}
