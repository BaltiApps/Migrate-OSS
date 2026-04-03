package baltiapps.migrate.domain.backup.sources

import baltiapps.migrate.domain.common.model.DataItem
import baltiapps.migrate.domain.common.model.GenericFile
import baltiapps.migrate.domain.common.model.Progress
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onCompletion

fun <T: DataItem<*>> backupEngineRunner(
    backupEngine: BackupEngine<T>,
    location: GenericFile,
    items: List<T>,
): Flow<Progress> {
    return backupEngineRunner(backupEngine, items) {
        backupEngine.setLocation(location)
    }
}

fun <T: DataItem<*>> backupEngineRunner(
    backupEngine: BackupEngine<T>,
    location: String,
    items: List<T>,
): Flow<Progress> {
    return backupEngineRunner(backupEngine, items) {
        backupEngine.setLocation(location)
    }
}

private fun <T: DataItem<*>> backupEngineRunner(
    backupEngine: BackupEngine<T>,
    items: List<T>,
    initializer: () -> Unit,
): Flow<Progress> {
    return try {
        initializer()
        backupEngine.backupDataItems(items).onCompletion {
            runCatching { backupEngine.onBackupOver() }
        }
    } catch (e: Exception) {
        e.printStackTrace()
        runCatching { backupEngine.onBackupOver() }
        flowOf()
    }
}