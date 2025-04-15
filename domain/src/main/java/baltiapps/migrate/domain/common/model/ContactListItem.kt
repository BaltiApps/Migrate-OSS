package baltiapps.migrate.domain.common.model

data class ContactListItem(
    override val _id: String,
    val displayName: String,
    val displayNumber: String,
    val isLocalContact : Boolean,
    val isChecked: Boolean,
): ListItem