package baltiapps.migrate.domain.restore.sources

import baltiapps.migrate.domain.common.model.DataItem
import baltiapps.migrate.domain.common.model.Progress
import kotlinx.coroutines.flow.Flow

interface DataRestore<T: DataItem<*>> {
    fun checkPermission(): Boolean
    fun restoreDataItems(items: List<T>): Flow<Progress>
}