package baltiapps.migrate.domain.common.model

interface DataItem<T: ListItem> {
    val _id: String
    val logInfo: String
    fun toListItem(): T
}