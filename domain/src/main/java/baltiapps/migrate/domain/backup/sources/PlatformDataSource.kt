package baltiapps.migrate.domain.backup.sources

import baltiapps.migrate.domain.backup.model.DataItem
import baltiapps.migrate.domain.backup.model.Progress
import kotlinx.coroutines.flow.Flow

interface PlatformDataSource<T: DataItem<*>> {
    fun checkPermission(): Boolean
    suspend fun getData(
        onFinishedLoading: (List<T>) -> Unit,
    ): Flow<Progress>
}