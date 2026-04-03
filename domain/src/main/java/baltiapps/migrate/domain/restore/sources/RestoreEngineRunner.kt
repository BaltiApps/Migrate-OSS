package baltiapps.migrate.domain.restore.sources

import baltiapps.migrate.domain.common.model.DataItem
import baltiapps.migrate.domain.common.model.Progress
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onCompletion
import java.util.concurrent.CancellationException

fun <T: DataItem<*>> restoreEngineRunner(
    restoreEngine: RestoreEngine<T>,
    items: List<T>,
): Flow<Progress> {
    return restoreEngineRunner(restoreEngine, items) {}
}

fun <T: DataItem<*>> restoreEngineRunner(
    restoreEngine: RestoreEngine<T>,
    location: String,
    items: List<T>,
): Flow<Progress> {
    return restoreEngineRunner(restoreEngine, items) {
        restoreEngine.setLocation(location)
    }
}

private fun <T: DataItem<*>> restoreEngineRunner(
    restoreEngine: RestoreEngine<T>,
    items: List<T>,
    initializer: () -> Unit,
): Flow<Progress> {
    return try {
        initializer()
        restoreEngine.restoreDataItems(items).onCompletion {
            runCatching { restoreEngine.onRestoreOver() }
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        e.printStackTrace()
        runCatching { restoreEngine.onRestoreOver() }
        flowOf()
    }
}