package baltiapps.migrate.domain.backup.sources

import baltiapps.migrate.domain.common.model.DataItem
import baltiapps.migrate.domain.common.model.Progress
import kotlinx.coroutines.flow.Flow

interface DataSource<T: DataItem<*>> {
    fun checkPermission(): Boolean
    suspend fun getData(
        onFinishedLoading: (List<T>) -> Unit,
    ): Flow<Progress>
}