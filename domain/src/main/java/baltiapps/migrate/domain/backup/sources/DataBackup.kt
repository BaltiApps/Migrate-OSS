package baltiapps.migrate.domain.backup.sources

import baltiapps.migrate.domain.common.model.DataItem
import baltiapps.migrate.domain.common.model.GenericFile
import baltiapps.migrate.domain.common.model.Progress
import kotlinx.coroutines.flow.Flow

interface DataBackup<T: DataItem<*>> {
    fun setLocation(location: String) {}
    fun setLocation(file: GenericFile) {}
    fun backupDataItems(dataItems: List<T>): Flow<Progress>
    fun onBackupOver()
}