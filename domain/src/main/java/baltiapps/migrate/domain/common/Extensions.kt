package baltiapps.migrate.domain.common

import baltiapps.migrate.domain.common.model.DataItem
import baltiapps.migrate.domain.common.model.ListItem

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