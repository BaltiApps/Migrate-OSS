package baltiapps.migrate.domain.common.converter

import baltiapps.migrate.domain.common.model.AppListItem
import baltiapps.migrate.domain.common.model.DataItem

interface AppListItemToDataItemConverter<T: DataItem<AppListItem>> {
    fun convertSelection(listItem: AppListItem, dataItem: T): T
}