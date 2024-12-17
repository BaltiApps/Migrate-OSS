package baltiapps.migrate.domain.backup.model

data class ContactListItem(
    override val _id: String,
    val displayName: String,
    val displayNumber: String,
    val isChecked: Boolean,
): ListItem