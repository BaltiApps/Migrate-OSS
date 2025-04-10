package baltiapps.migrate.domain.common

import baltiapps.migrate.domain.common.model.DataItem
import baltiapps.migrate.domain.common.model.ListItem
import baltiapps.migrate.domain.common.model.Progress

fun <T> MutableList<T>.clearAndAddAll(items: List<T>) {
    this.clear()
    this.addAll(items)
}

fun <T: ListItem, V: DataItem<T>> List<V>.toListItems(): List<T> {
    return this.map { it.toListItem() }
}

fun getPercentage(count: Number, total: Number): Double {
    return (count.toDouble() / total.toDouble())
}

fun runCatchingWithProgress(
    progress: Progress,
    block: () -> Unit,
): Progress {
    return try {
        block()
        progress
    } catch (e: Exception) {
        progress.copy(
            logs = progress.logs.run {
                if (isBlank()) {
                    e.message.toString()
                } else "$this - ${e.message}"
            },
            isFailure = true
        )
    }
}