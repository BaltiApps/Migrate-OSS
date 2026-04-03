package baltiapps.migrate.domain.restore.sources

import baltiapps.migrate.domain.common.model.DataItem
import baltiapps.migrate.domain.common.model.GenericFile
import baltiapps.migrate.domain.common.model.Progress
import kotlinx.coroutines.flow.Flow

interface RestoreReader<T: DataItem<*>> {
    fun setup(file: GenericFile)
    fun readItems(onFinished: (List<T>) -> Unit): Flow<Progress>
    fun close()
}