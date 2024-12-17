package baltiapps.migrate.domain.backup.model

interface DataItem<T: ListItem> {
    val _id: String
    val logInfo: String
    fun toListItem(): T
}