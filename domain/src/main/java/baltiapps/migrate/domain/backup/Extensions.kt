package baltiapps.migrate.domain.backup

import baltiapps.migrate.domain.backup.model.DataItem
import baltiapps.migrate.domain.backup.model.ListItem

fun <T: ListItem, V: DataItem<T>> List<V>.toListItems(): List<T> {
    return this.map { it.toListItem() }
}

fun getPercentage(count: Number, total: Number): Double {
    return (count.toDouble() / total.toDouble())
}