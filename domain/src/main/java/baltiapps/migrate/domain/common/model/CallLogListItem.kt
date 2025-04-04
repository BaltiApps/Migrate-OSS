package baltiapps.migrate.domain.common.model

data class CallLogListItem(
    override val _id: String,
    val callStatus: Int,
    val displayName: String,
    val displayNumber: String,
    val creationDate: ItemCreationDate,
    val isChecked: Boolean,
): ListItem