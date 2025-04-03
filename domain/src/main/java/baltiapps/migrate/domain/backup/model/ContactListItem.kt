package baltiapps.migrate.domain.backup.model

import baltiapps.migrate.domain.common.model.ListItem

data class ContactListItem(
    override val _id: String,
    val displayName: String,
    val displayNumber: String,
    val isChecked: Boolean,
): ListItem