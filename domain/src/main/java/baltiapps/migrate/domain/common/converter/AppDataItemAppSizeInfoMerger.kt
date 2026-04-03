package baltiapps.migrate.domain.common.converter

import baltiapps.migrate.domain.common.model.AppListItem
import baltiapps.migrate.domain.common.model.AppSizeInfo
import baltiapps.migrate.domain.common.model.DataItem

interface AppDataItemAppSizeInfoMerger<T: DataItem<AppListItem>> {
    fun merge(appDataItem: T, appSizeInfo: AppSizeInfo?): T
}
