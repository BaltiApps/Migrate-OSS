package baltiapps.migrate.domain.common.usecase

import baltiapps.migrate.domain.common.clearAndAddAll
import baltiapps.migrate.domain.common.converter.AppListItemToDataItemConverter
import baltiapps.migrate.domain.common.model.AppListItem
import baltiapps.migrate.domain.common.model.DataItem
import baltiapps.migrate.domain.common.repository.DataRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class StageSelectedApps(
    private val appListItemToDataItemConverter: AppListItemToDataItemConverter<DataItem<AppListItem>>
) {
    suspend operator fun invoke(
        allListItems: List<AppListItem>,
        dataRepository: DataRepository,
    ) {
        withContext(Dispatchers.Default) {
            val stagedItems = mutableListOf<DataItem<AppListItem>>()

            allListItems.forEach { listItem ->
                val correspondingDataItem = dataRepository.appDataItems.find { it._id == listItem._id }

                if (correspondingDataItem != null && listItem.isAnySelected()) {
                    stagedItems.add(
                        appListItemToDataItemConverter.convertSelection(
                            listItem = listItem,
                            dataItem = correspondingDataItem,
                        )
                    )
                }
            }

            dataRepository.stagedApps.clearAndAddAll(stagedItems)
        }
    }
}