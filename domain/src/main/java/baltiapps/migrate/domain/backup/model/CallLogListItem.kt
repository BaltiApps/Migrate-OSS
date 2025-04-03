package baltiapps.migrate.domain.backup.model

import baltiapps.migrate.domain.common.model.ItemCreationDate
import baltiapps.migrate.domain.common.model.ListItem

data class CallLogListItem(
    override val _id: String,
    val callStatus: Int,
    val displayName: String,
    val displayNumber: String,
    val creationDate: ItemCreationDate,
    val isChecked: Boolean,
): ListItem