package baltiapps.migrate.domain.restore.usecase

import baltiapps.migrate.domain.common.clearAndAddAll
import baltiapps.migrate.domain.common.converter.AppListItemToDataItemConverter
import baltiapps.migrate.domain.common.getPercentage
import baltiapps.migrate.domain.common.model.AppListItem
import baltiapps.migrate.domain.common.model.DataItem
import baltiapps.migrate.domain.common.model.DrawableAsset
import baltiapps.migrate.domain.common.model.Progress
import baltiapps.migrate.domain.common.sources.fileSystem.TextReader
import baltiapps.migrate.domain.restore.repository.RestoreDataRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class ReadAppListForRestoreUseCase(
    private val appIconReader: TextReader<DrawableAsset>,
    private val appInfoReader: TextReader<DataItem<AppListItem>>,
    private val restoreDataRepository: RestoreDataRepository,
    private val appListItemToDataItemConverter: AppListItemToDataItemConverter<DataItem<AppListItem>>
) {
    operator fun invoke(): Flow<Progress> {
        return flow {

            val appInfoFiles = restoreDataRepository.getAppInfoFiles()
            val appIconFiles = restoreDataRepository.getAppIconFiles()

            val appDataItemsForRepository = mutableListOf<DataItem<AppListItem>>()

            appInfoFiles.forEachIndexed { index, file ->

                val fileNameWithoutExtension = file.name.substringBeforeLast('.')

                appInfoReader.setup(file.parentPath, file.name)
                val readAppData = appInfoReader.read()

                val readAppIcon = appIconFiles.find {
                    it.name.substringBeforeLast('.') == fileNameWithoutExtension
                }?.run {
                    appIconReader.setup(parentPath, name)
                    appIconReader.read()
                }

                val appDataToStoreInRepository = if (readAppIcon != null) {
                    val tempAppListItem = readAppData.toListItem().copy(
                        appIcon = readAppIcon,
                    )
                    val appDataWithIcon = appListItemToDataItemConverter.setIcon(
                        listItem = tempAppListItem,
                        dataItem = readAppData
                    )
                    appDataWithIcon
                } else {
                    readAppData
                }

                appDataItemsForRepository.add(appDataToStoreInRepository)

                emit(Progress(
                    itemId = appDataToStoreInRepository._id,
                    logs = appDataToStoreInRepository.logInfo,
                    progressType = Progress.ProgressType.APP_INFO_READ,
                    percentage = getPercentage(index+1, appInfoFiles.size)
                ))
            }

            restoreDataRepository.appDataItems.clearAndAddAll(appDataItemsForRepository)
        }
    }
}