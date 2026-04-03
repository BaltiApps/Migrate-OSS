package baltiapps.migrate.domain.restore.sources

import baltiapps.migrate.domain.common.model.DataItem
import baltiapps.migrate.domain.common.model.GenericFile
import baltiapps.migrate.domain.common.model.Progress
import kotlinx.coroutines.flow.Flow

fun <T: DataItem<*>> restoreReaderRunner(
    file: GenericFile,
    restoreReader: RestoreReader<T>,
    onItemsRead: (List<T>) -> Unit,
): Flow<Progress> {
    restoreReader.setup(file)
    return restoreReader.readItems { results ->
        onItemsRead(results)
        restoreReader.close()
    }
}