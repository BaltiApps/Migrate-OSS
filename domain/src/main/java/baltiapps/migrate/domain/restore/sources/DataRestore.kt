package baltiapps.migrate.domain.restore.sources

import baltiapps.migrate.domain.common.model.DataItem

interface DataRestore<T: DataItem<*>> {
    fun checkPermission(): Boolean
    fun restoreDataItem(dataItem: T)
}