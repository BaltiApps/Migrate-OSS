package baltiapps.migrate.domain.backup

import baltiapps.migrate.domain.common.getPercentage
import baltiapps.migrate.domain.common.model.DataItem
import baltiapps.migrate.domain.common.model.Progress
import kotlinx.coroutines.flow.FlowCollector

suspend fun <T : DataItem<*>> FlowCollector<Progress>.tryPerformWrite(
    items: List<T>,
    progressType: Progress.ProgressType,
    writeBlock: (item: T) -> Unit,
) {
    items.forEachIndexed { index, item ->
        val progress = Progress(
            progressType = progressType,
            percentage = getPercentage(index + 1, items.size),
            logs = "(${index + 1}/${items.size}) ${item.logInfo}"
        )
        emit(
            try {
                writeBlock(item)
                progress
            } catch (e: Exception) {
                progress.copy(
                    logs = "${item.logInfo} - ${e.message}",
                    isFailure = true
                )
            }
        )
    }
}
