package baltiapps.migrate.domain.backup.usecase

import baltiapps.migrate.domain.backup.repository.BackupDataRepository
import baltiapps.migrate.domain.common.converter.AppDataItemAppSizeInfoMerger
import baltiapps.migrate.domain.common.getPercentage
import baltiapps.migrate.domain.common.model.AppListItem
import baltiapps.migrate.domain.common.model.DataItem
import baltiapps.migrate.domain.common.model.Progress
import baltiapps.migrate.domain.common.sources.fileSystem.TextWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

class BackupAppsInfoUseCase(
    private val appIconWriter: TextWriter<DataItem<AppListItem>>,
    private val appInfoWriter: TextWriter<DataItem<AppListItem>>,
    private val appDataItemAppSizeInfoMerger: AppDataItemAppSizeInfoMerger<DataItem<AppListItem>>,
    private val dataRepository: BackupDataRepository,
) {
    operator fun invoke(
        internalBackupPath: String,
    ): Flow<Progress> {
        return flow {
            dataRepository.stagedApps.forEachIndexed { index, appDataItem ->

                val appSizeInfo = dataRepository.stagedAppSizes.find { it.packageName == appDataItem._id }
                val appDataItem = appDataItemAppSizeInfoMerger.merge(appDataItem, appSizeInfo)

                appInfoWriter.setup(
                    fileLocation = internalBackupPath,
                    fileName = "${appDataItem._id}.json",
                    append = false
                )
                appInfoWriter.write(appDataItem)
                appIconWriter.setup(
                    fileLocation = internalBackupPath,
                    fileName = "${appDataItem._id}.mpng",
                    append = false
                )
                appIconWriter.write(appDataItem)

                val appName = appDataItem.logInfo.substringBeforeLast(':').trim()

                emit(Progress(
                    logs = appName,
                    progressType = Progress.ProgressType.APP_INFO_BACKUP,
                    percentage = getPercentage(index+1, dataRepository.stagedApps.size),
                ))
            }
        }.flowOn(Dispatchers.IO)
    }
}