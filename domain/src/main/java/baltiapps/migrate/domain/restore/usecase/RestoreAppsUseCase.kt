package baltiapps.migrate.domain.restore.usecase

import baltiapps.migrate.domain.common.model.AppListItem
import baltiapps.migrate.domain.common.model.DataItem
import baltiapps.migrate.domain.common.model.GenericFile
import baltiapps.migrate.domain.common.model.Progress
import baltiapps.migrate.domain.common.sources.fileSystem.FileSystemSource
import baltiapps.migrate.domain.restore.repository.RestoreDataRepository
import baltiapps.migrate.domain.restore.sources.DataRestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class RestoreAppsUseCase(
    private val fileSystemSource: FileSystemSource,
    private val appRestoreEngine: DataRestore<DataItem<AppListItem>>,
    private val dataRepository: RestoreDataRepository,
) {
    operator fun invoke(
        source: GenericFile,
    ): Flow<Progress> {
        return try {
            appRestoreEngine.setLocation(
                fileSystemSource.getLocalFilePath(source) ?: source.path
            )
            appRestoreEngine.restoreDataItems(dataRepository.stagedApps)
        } catch (e: Exception) {
            e.printStackTrace()
            flowOf()
        } finally {
            runCatching {
                appRestoreEngine.onRestoreOver()
            }
        }
    }
}
