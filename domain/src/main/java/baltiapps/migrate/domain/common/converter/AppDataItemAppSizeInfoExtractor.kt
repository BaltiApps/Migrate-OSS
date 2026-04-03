package baltiapps.migrate.domain.common.converter

import baltiapps.migrate.domain.common.model.AppListItem
import baltiapps.migrate.domain.common.model.AppSizeInfo
import baltiapps.migrate.domain.common.model.DataItem

interface AppDataItemAppSizeInfoExtractor<T: DataItem<AppListItem>> {
    fun extract(appDataItem: T): AppSizeInfo
}
