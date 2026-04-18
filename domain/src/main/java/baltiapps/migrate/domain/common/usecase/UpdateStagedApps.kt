package baltiapps.migrate.domain.common.usecase

import baltiapps.migrate.domain.common.clearAndAddAll
import baltiapps.migrate.domain.common.converter.AppListItemToDataItemConverter
import baltiapps.migrate.domain.common.model.AppListItem
import baltiapps.migrate.domain.common.model.DataItem
import baltiapps.migrate.domain.common.repository.DataRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UpdateStagedApps(
    private val appListItemToDataItemConverter: AppListItemToDataItemConverter<DataItem<AppListItem>>
) {
    suspend operator fun invoke(
        listItems: List<AppListItem>,
        dataRepository: DataRepository,
    ) {
        withContext(Dispatchers.Default) {
            val updateMap = listItems.associateBy { it._id }
            val updated = dataRepository.stagedApps.map { item ->
                val listItem = updateMap[item._id] ?: return@map item
                appListItemToDataItemConverter.convertSelection(
                    listItem = listItem,
                    dataItem = item,
                )
            }
            dataRepository.stagedApps.clearAndAddAll(updated)
        }
    }
}
